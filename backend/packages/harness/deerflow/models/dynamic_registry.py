"""Encrypted dynamic provider/model registry for the moataz ai product layer.

The upstream YAML model configuration remains untouched.  Records created from
UI/API are stored separately and are resolved only when a dynamic model name is
requested.  Provider credentials are encrypted with a master key supplied by
the deployment environment; plaintext keys are never serialized.
"""

from __future__ import annotations

import base64
import hashlib
import json
import os
import re
import tempfile
import threading
import uuid
from datetime import UTC, datetime
from pathlib import Path
from typing import Any, Literal

from pydantic import BaseModel, Field

from deerflow.config.model_config import ModelConfig
from deerflow.config.paths import get_paths

ProviderType = Literal[
    "openai",
    "openrouter",
    "openai_compatible",
    "ollama",
    "vllm",
    "lmstudio",
    "litellm",
]

_REGISTRY_FILENAME = "moataz_dynamic_providers.json"
_MASTER_KEY_ENV = "MOATAZ_PROVIDER_MASTER_KEY"
_LOCK = threading.RLock()


class ProviderRegistryError(RuntimeError):
    pass


class ProviderSecretError(ProviderRegistryError):
    pass


class DynamicModelRecord(BaseModel):
    registry_name: str
    model: str
    display_name: str | None = None
    description: str | None = None
    supports_thinking: bool = False
    supports_reasoning_effort: bool = False
    supports_vision: bool = False
    supports_tools: bool = True
    supports_streaming: bool = True
    context_window: int | None = Field(default=None, gt=0)


class DynamicProviderRecord(BaseModel):
    id: str
    name: str
    provider_type: ProviderType
    base_url: str
    encrypted_api_key: str | None = None
    models: list[DynamicModelRecord] = Field(default_factory=list)
    created_at: str
    updated_at: str


class ProviderSummary(BaseModel):
    id: str
    name: str
    provider_type: ProviderType
    base_url: str
    has_api_key: bool
    models: list[DynamicModelRecord]
    created_at: str
    updated_at: str


class RegistryDocument(BaseModel):
    version: int = 1
    default_model: str | None = None
    providers: list[DynamicProviderRecord] = Field(default_factory=list)


def _registry_path() -> Path:
    path = get_paths().base_dir / _REGISTRY_FILENAME
    path.parent.mkdir(parents=True, exist_ok=True)
    return path


def _now() -> str:
    return datetime.now(UTC).isoformat()


def _read_document() -> RegistryDocument:
    path = _registry_path()
    if not path.exists():
        return RegistryDocument()
    try:
        return RegistryDocument.model_validate_json(path.read_text(encoding="utf-8"))
    except Exception as exc:
        raise ProviderRegistryError("Dynamic provider registry is unreadable or invalid") from exc


def _write_document(document: RegistryDocument) -> None:
    path = _registry_path()
    payload = document.model_dump_json(indent=2)
    fd, temp_name = tempfile.mkstemp(prefix=f".{_REGISTRY_FILENAME}.", dir=path.parent)
    try:
        os.fchmod(fd, 0o600)
        with os.fdopen(fd, "w", encoding="utf-8") as handle:
            handle.write(payload)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temp_name, path)
        try:
            os.chmod(path, 0o600)
        except OSError:
            pass
    finally:
        try:
            os.unlink(temp_name)
        except FileNotFoundError:
            pass


def _fernet():
    master = os.getenv(_MASTER_KEY_ENV)
    if not master or len(master.encode("utf-8")) < 32:
        raise ProviderSecretError(
            f"{_MASTER_KEY_ENV} must be set to a high-entropy value of at least 32 bytes before provider credentials can be saved"
        )
    try:
        from cryptography.fernet import Fernet
    except ImportError as exc:
        raise ProviderSecretError(
            "The 'cryptography' package is required for encrypted provider credentials; refusing plaintext storage"
        ) from exc
    digest = hashlib.sha256(master.encode("utf-8")).digest()
    return Fernet(base64.urlsafe_b64encode(digest))


def _encrypt_secret(secret: str | None) -> str | None:
    if not secret:
        return None
    return _fernet().encrypt(secret.encode("utf-8")).decode("ascii")


def _decrypt_secret(ciphertext: str | None) -> str | None:
    if not ciphertext:
        return None
    try:
        return _fernet().decrypt(ciphertext.encode("ascii")).decode("utf-8")
    except ProviderSecretError:
        raise
    except Exception as exc:
        raise ProviderSecretError("Provider credential could not be decrypted with the configured master key") from exc


def _slug(value: str) -> str:
    slug = re.sub(r"[^a-zA-Z0-9._-]+", "-", value).strip("-._").lower()
    return slug[:64] or "model"


def build_registry_model_name(provider_id: str, model_id: str) -> str:
    return f"dyn-{provider_id[:8]}-{_slug(model_id)}"


def list_provider_summaries() -> list[ProviderSummary]:
    with _LOCK:
        doc = _read_document()
        return [
            ProviderSummary(
                id=item.id,
                name=item.name,
                provider_type=item.provider_type,
                base_url=item.base_url,
                has_api_key=bool(item.encrypted_api_key),
                models=item.models,
                created_at=item.created_at,
                updated_at=item.updated_at,
            )
            for item in doc.providers
        ]


def get_provider(provider_id: str) -> DynamicProviderRecord | None:
    with _LOCK:
        return next((item for item in _read_document().providers if item.id == provider_id), None)


def save_provider(
    *,
    name: str,
    provider_type: ProviderType,
    base_url: str,
    api_key: str | None,
    models: list[dict[str, Any]],
    provider_id: str | None = None,
    make_default: str | None = None,
) -> ProviderSummary:
    with _LOCK:
        doc = _read_document()
        existing = next((item for item in doc.providers if item.id == provider_id), None) if provider_id else None
        pid = existing.id if existing else uuid.uuid4().hex
        now = _now()
        encrypted = _encrypt_secret(api_key) if api_key else (existing.encrypted_api_key if existing else None)
        normalized_models: list[DynamicModelRecord] = []
        for item in models:
            model_id = str(item.get("model") or item.get("id") or "").strip()
            if not model_id:
                continue
            normalized_models.append(
                DynamicModelRecord(
                    registry_name=str(item.get("registry_name") or build_registry_model_name(pid, model_id)),
                    model=model_id,
                    display_name=item.get("display_name") or item.get("name") or model_id,
                    description=item.get("description"),
                    supports_thinking=bool(item.get("supports_thinking", False)),
                    supports_reasoning_effort=bool(item.get("supports_reasoning_effort", False)),
                    supports_vision=bool(item.get("supports_vision", False)),
                    supports_tools=bool(item.get("supports_tools", True)),
                    supports_streaming=bool(item.get("supports_streaming", True)),
                    context_window=item.get("context_window"),
                )
            )
        record = DynamicProviderRecord(
            id=pid,
            name=name.strip() or provider_type,
            provider_type=provider_type,
            base_url=base_url.rstrip("/"),
            encrypted_api_key=encrypted,
            models=normalized_models,
            created_at=existing.created_at if existing else now,
            updated_at=now,
        )
        if existing:
            doc.providers = [record if item.id == pid else item for item in doc.providers]
        else:
            doc.providers.append(record)
        if make_default and any(model.registry_name == make_default for model in normalized_models):
            doc.default_model = make_default
        elif doc.default_model and not any(
            model.registry_name == doc.default_model for provider in doc.providers for model in provider.models
        ):
            doc.default_model = None
        _write_document(doc)
        return ProviderSummary(
            id=record.id,
            name=record.name,
            provider_type=record.provider_type,
            base_url=record.base_url,
            has_api_key=bool(record.encrypted_api_key),
            models=record.models,
            created_at=record.created_at,
            updated_at=record.updated_at,
        )


def delete_provider(provider_id: str) -> bool:
    with _LOCK:
        doc = _read_document()
        before = len(doc.providers)
        removed_names = {
            model.registry_name
            for provider in doc.providers
            if provider.id == provider_id
            for model in provider.models
        }
        doc.providers = [item for item in doc.providers if item.id != provider_id]
        if len(doc.providers) == before:
            return False
        if doc.default_model in removed_names:
            doc.default_model = None
        _write_document(doc)
        return True


def set_default_dynamic_model(model_name: str | None) -> None:
    with _LOCK:
        doc = _read_document()
        if model_name is not None and not any(
            model.registry_name == model_name for provider in doc.providers for model in provider.models
        ):
            raise ProviderRegistryError("Dynamic model not found")
        doc.default_model = model_name
        _write_document(doc)


def get_default_dynamic_model_name() -> str | None:
    with _LOCK:
        return _read_document().default_model


def _to_model_config(provider: DynamicProviderRecord, model: DynamicModelRecord, *, include_credentials: bool) -> ModelConfig:
    api_key = _decrypt_secret(provider.encrypted_api_key) if include_credentials else None
    # ChatOpenAI works with OpenAI, OpenRouter, LiteLLM, LM Studio, modern Ollama
    # /v1 compatibility and vLLM.  Provider-specific discovery stays in Gateway.
    return ModelConfig(
        name=model.registry_name,
        display_name=model.display_name,
        description=model.description or f"Dynamic {provider.name} model",
        use="langchain_openai.ChatOpenAI",
        model=model.model,
        base_url=provider.base_url,
        api_key=api_key or "local-no-key",
        supports_thinking=model.supports_thinking,
        supports_reasoning_effort=model.supports_reasoning_effort,
        supports_vision=model.supports_vision,
        context_window=model.context_window,
    )


def list_dynamic_model_configs(*, include_credentials: bool = False) -> list[ModelConfig]:
    with _LOCK:
        doc = _read_document()
        return [
            _to_model_config(provider, model, include_credentials=include_credentials)
            for provider in doc.providers
            for model in provider.models
        ]


def get_dynamic_model_config(name: str, *, include_credentials: bool = True) -> ModelConfig | None:
    with _LOCK:
        for provider in _read_document().providers:
            for model in provider.models:
                if model.registry_name == name:
                    return _to_model_config(provider, model, include_credentials=include_credentials)
    return None


def is_dynamic_model_name(name: str | None) -> bool:
    return bool(name and name.startswith("dyn-"))


__all__ = [
    "DynamicModelRecord",
    "DynamicProviderRecord",
    "ProviderRegistryError",
    "ProviderSecretError",
    "ProviderSummary",
    "ProviderType",
    "build_registry_model_name",
    "delete_provider",
    "get_default_dynamic_model_name",
    "get_dynamic_model_config",
    "get_provider",
    "is_dynamic_model_name",
    "list_dynamic_model_configs",
    "list_provider_summaries",
    "save_provider",
    "set_default_dynamic_model",
]
