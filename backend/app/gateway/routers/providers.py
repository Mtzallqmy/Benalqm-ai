from __future__ import annotations

import asyncio
import ipaddress
import os
import socket
from typing import Any, Literal
from urllib.parse import urlsplit

import httpx
from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field, SecretStr

from app.gateway.deps import require_admin_user
from deerflow.models.dynamic_registry import (
    ProviderRegistryError,
    ProviderSecretError,
    ProviderSummary,
    ProviderType,
    delete_provider,
    get_default_dynamic_model_name,
    save_provider,
    set_default_dynamic_model,
)

router = APIRouter(prefix="/providers", tags=["providers"])

_PROVIDER_PRESETS: dict[str, str | None] = {
    "openai": "https://api.openai.com/v1",
    "openrouter": "https://openrouter.ai/api/v1",
    "ollama": "http://host.docker.internal:11434",
    "lmstudio": "http://host.docker.internal:1234/v1",
    "vllm": None,
    "litellm": None,
    "openai_compatible": None,
}


class ProviderConnectionRequest(BaseModel):
    provider_type: ProviderType
    base_url: str | None = None
    api_key: SecretStr | None = None
    timeout_seconds: float = Field(default=12, ge=2, le=60)


class ProviderSaveRequest(ProviderConnectionRequest):
    name: str = Field(min_length=1, max_length=80)
    provider_id: str | None = None
    models: list[dict[str, Any]] = Field(default_factory=list)
    default_model: str | None = None


class ProviderDefaultRequest(BaseModel):
    model_name: str | None = None


class ProviderProbeRequest(ProviderConnectionRequest):
    model: str


class ProviderModelsResponse(BaseModel):
    models: list[dict[str, Any]]


class ProviderTestResponse(BaseModel):
    ok: bool
    model_count: int


def _resolved_base_url(provider_type: ProviderType, supplied: str | None) -> str:
    preset = _PROVIDER_PRESETS[provider_type]
    value = preset or (supplied or "").strip()
    if not value:
        raise HTTPException(status_code=422, detail="Base URL is required for this provider type")
    return value.rstrip("/")


def _is_forbidden_ip(value: str) -> bool:
    try:
        ip = ipaddress.ip_address(value)
    except ValueError:
        return False
    return ip.is_loopback or ip.is_private or ip.is_link_local or ip.is_multicast or ip.is_reserved or ip.is_unspecified


def _validate_network_target(url: str) -> None:
    parsed = urlsplit(url)
    if parsed.scheme not in {"http", "https"} or not parsed.hostname or parsed.username or parsed.password:
        raise HTTPException(status_code=422, detail="Provider URL must be an http(s) URL without embedded credentials")

    policy = os.getenv("MOATAZ_PROVIDER_NETWORK_POLICY", "hosted").strip().lower()
    if policy == "local":
        return
    if policy != "hosted":
        raise HTTPException(status_code=503, detail="Invalid MOATAZ_PROVIDER_NETWORK_POLICY; use 'hosted' or 'local'")

    hostname = parsed.hostname.lower()
    if hostname in {"localhost", "host.docker.internal"} or hostname.endswith(".local") or _is_forbidden_ip(hostname):
        raise HTTPException(status_code=422, detail="Private/local provider targets are blocked in hosted network policy")
    try:
        addresses = socket.getaddrinfo(hostname, parsed.port or (443 if parsed.scheme == "https" else 80), type=socket.SOCK_STREAM)
    except socket.gaierror as exc:
        raise HTTPException(status_code=422, detail="Provider hostname could not be resolved") from exc
    if any(_is_forbidden_ip(address[4][0]) for address in addresses):
        raise HTTPException(status_code=422, detail="Provider resolves to a private or reserved network address")


def _headers(api_key: str | None, provider_type: ProviderType) -> dict[str, str]:
    headers = {"Accept": "application/json"}
    if api_key:
        headers["Authorization"] = f"Bearer {api_key}"
    if provider_type == "openrouter":
        headers["HTTP-Referer"] = "https://github.com/Mtzallqmy/Benalqm-ai"
        headers["X-Title"] = "moataz ai"
    return headers


def _safe_status_detail(status: int) -> str:
    if status == 401:
        return "Provider rejected the API key (401)"
    if status == 403:
        return "Provider denied access (403)"
    if status == 404:
        return "Provider endpoint or model was not found (404)"
    if status == 429:
        return "Provider rate limit reached (429)"
    return f"Provider returned HTTP {status}"


def _infer_capabilities(model_id: str, metadata: dict[str, Any] | None = None) -> dict[str, Any]:
    metadata = metadata or {}
    lowered = model_id.lower()
    architecture = metadata.get("architecture") if isinstance(metadata.get("architecture"), dict) else {}
    modalities = architecture.get("input_modalities") or metadata.get("input_modalities") or []
    modalities_text = " ".join(str(item).lower() for item in modalities)
    reasoning = any(token in lowered for token in ("o1", "o3", "o4", "gpt-5", "reason", "r1", "qwen3"))
    vision = any(token in lowered for token in ("vision", "-vl", "4o", "gpt-5", "gemini")) or "image" in modalities_text
    context = metadata.get("context_length")
    try:
        context_window = int(context) if context else None
    except (TypeError, ValueError):
        context_window = None
    return {
        "supports_thinking": reasoning,
        "supports_reasoning_effort": reasoning and any(token in lowered for token in ("o1", "o3", "o4", "gpt-5")),
        "supports_vision": vision,
        "supports_tools": True,
        "supports_streaming": True,
        "context_window": context_window,
    }


async def _discover(request: ProviderConnectionRequest) -> tuple[str, list[dict[str, Any]]]:
    base_url = _resolved_base_url(request.provider_type, request.base_url)
    _validate_network_target(base_url)
    api_key = request.api_key.get_secret_value() if request.api_key else None
    timeout = httpx.Timeout(request.timeout_seconds)
    try:
        async with httpx.AsyncClient(timeout=timeout, follow_redirects=False) as client:
            if request.provider_type == "ollama":
                native_base = base_url.removesuffix("/v1")
                response = await client.get(f"{native_base}/api/tags", headers=_headers(api_key, request.provider_type))
                if response.status_code >= 400:
                    raise HTTPException(status_code=502, detail=_safe_status_detail(response.status_code))
                data = response.json()
                raw_models = data.get("models", []) if isinstance(data, dict) else []
                models = []
                for item in raw_models:
                    model_id = item.get("name") if isinstance(item, dict) else None
                    if model_id:
                        models.append({"model": model_id, "display_name": model_id, **_infer_capabilities(model_id, item)})
                return f"{native_base}/v1", models

            response = await client.get(f"{base_url}/models", headers=_headers(api_key, request.provider_type))
            if response.status_code >= 400:
                raise HTTPException(status_code=502, detail=_safe_status_detail(response.status_code))
            data = response.json()
            raw_models = data.get("data", []) if isinstance(data, dict) else []
            models = []
            for item in raw_models:
                if not isinstance(item, dict):
                    continue
                model_id = str(item.get("id") or item.get("name") or "").strip()
                if model_id:
                    models.append({"model": model_id, "display_name": item.get("name") or model_id, **_infer_capabilities(model_id, item)})
            return base_url, models
    except HTTPException:
        raise
    except httpx.TimeoutException as exc:
        raise HTTPException(status_code=504, detail="Provider connection timed out") from exc
    except (httpx.HTTPError, ValueError) as exc:
        raise HTTPException(status_code=502, detail="Provider connection failed") from exc


async def _require_admin(request: Request) -> None:
    await require_admin_user(request, detail="Admin privileges are required to manage AI providers")


@router.get("", response_model=list[ProviderSummary])
async def list_providers(request: Request) -> list[ProviderSummary]:
    await _require_admin(request)
    from deerflow.models.dynamic_registry import list_provider_summaries

    return await asyncio.to_thread(list_provider_summaries)


@router.get("/default")
async def get_default_provider_model(request: Request) -> dict[str, str | None]:
    await _require_admin(request)
    return {"model_name": await asyncio.to_thread(get_default_dynamic_model_name)}


@router.post("/test", response_model=ProviderTestResponse)
async def test_provider(payload: ProviderConnectionRequest, request: Request) -> ProviderTestResponse:
    await _require_admin(request)
    _, models = await _discover(payload)
    return ProviderTestResponse(ok=True, model_count=len(models))


@router.post("/discover", response_model=ProviderModelsResponse)
async def discover_provider_models(payload: ProviderConnectionRequest, request: Request) -> ProviderModelsResponse:
    await _require_admin(request)
    _, models = await _discover(payload)
    return ProviderModelsResponse(models=models)


@router.post("/probe")
async def probe_provider_model(payload: ProviderProbeRequest, request: Request) -> dict[str, Any]:
    await _require_admin(request)
    base_url, _ = await _discover(payload)
    api_key = payload.api_key.get_secret_value() if payload.api_key else None
    try:
        async with httpx.AsyncClient(timeout=httpx.Timeout(payload.timeout_seconds), follow_redirects=False) as client:
            response = await client.post(
                f"{base_url}/chat/completions",
                headers={**_headers(api_key, payload.provider_type), "Content-Type": "application/json"},
                json={"model": payload.model, "messages": [{"role": "user", "content": "Reply only with OK."}], "stream": False},
            )
        if response.status_code >= 400:
            raise HTTPException(status_code=502, detail=_safe_status_detail(response.status_code))
        return {"ok": True, "auth": True, "chat": True, **_infer_capabilities(payload.model)}
    except HTTPException:
        raise
    except httpx.TimeoutException as exc:
        raise HTTPException(status_code=504, detail="Provider chat probe timed out") from exc
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail="Provider chat probe failed") from exc


@router.post("", response_model=ProviderSummary)
async def create_or_update_provider(payload: ProviderSaveRequest, request: Request) -> ProviderSummary:
    await _require_admin(request)
    base_url, discovered = await _discover(payload)
    discovered_by_id = {str(item["model"]): item for item in discovered}
    selected = []
    for requested in payload.models:
        model_id = str(requested.get("model") or requested.get("id") or "").strip()
        if not model_id:
            continue
        selected.append({**discovered_by_id.get(model_id, {}), **requested, "model": model_id})
    if not selected:
        raise HTTPException(status_code=422, detail="Select at least one discovered model")
    try:
        return await asyncio.to_thread(
            save_provider,
            name=payload.name,
            provider_type=payload.provider_type,
            base_url=base_url,
            api_key=payload.api_key.get_secret_value() if payload.api_key else None,
            models=selected,
            provider_id=payload.provider_id,
            make_default=payload.default_model,
        )
    except ProviderSecretError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except ProviderRegistryError as exc:
        raise HTTPException(status_code=500, detail="Provider registry could not be updated") from exc


@router.put("/default")
async def update_default_provider_model(payload: ProviderDefaultRequest, request: Request) -> dict[str, str | None]:
    await _require_admin(request)
    try:
        await asyncio.to_thread(set_default_dynamic_model, payload.model_name)
    except ProviderRegistryError as exc:
        raise HTTPException(status_code=404, detail="Dynamic model not found") from exc
    return {"model_name": payload.model_name}


@router.delete("/{provider_id}", status_code=204)
async def remove_provider(provider_id: str, request: Request) -> None:
    await _require_admin(request)
    removed = await asyncio.to_thread(delete_provider, provider_id)
    if not removed:
        raise HTTPException(status_code=404, detail="Provider not found")
