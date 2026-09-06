from __future__ import annotations

from pathlib import Path

import pytest
from fastapi import HTTPException

from app.gateway.routers.providers import _validate_network_target
from deerflow.models import dynamic_registry


def _isolated_registry(monkeypatch: pytest.MonkeyPatch, tmp_path: Path) -> Path:
    path = tmp_path / "providers.json"
    monkeypatch.setattr(dynamic_registry, "_registry_path", lambda: path)
    return path


def test_provider_registry_encrypts_api_key_at_rest(monkeypatch: pytest.MonkeyPatch, tmp_path: Path) -> None:
    pytest.importorskip("cryptography")
    path = _isolated_registry(monkeypatch, tmp_path)
    monkeypatch.setenv("MOATAZ_PROVIDER_MASTER_KEY", "a" * 48)
    summary = dynamic_registry.save_provider(
        name="test",
        provider_type="openai_compatible",
        base_url="https://example.com/v1",
        api_key="sk-super-secret-value",
        models=[{"model": "test-model"}],
    )
    assert summary.has_api_key is True
    assert "sk-super-secret-value" not in path.read_text(encoding="utf-8")
    config = dynamic_registry.get_dynamic_model_config(summary.models[0].registry_name)
    assert config is not None
    assert config.model_dump()["api_key"] == "sk-super-secret-value"


def test_provider_registry_refuses_plaintext_fallback(monkeypatch: pytest.MonkeyPatch, tmp_path: Path) -> None:
    _isolated_registry(monkeypatch, tmp_path)
    monkeypatch.delenv("MOATAZ_PROVIDER_MASTER_KEY", raising=False)
    with pytest.raises(dynamic_registry.ProviderSecretError):
        dynamic_registry.save_provider(
            name="test",
            provider_type="openai_compatible",
            base_url="https://example.com/v1",
            api_key="secret",
            models=[{"model": "test-model"}],
        )


def test_hosted_provider_policy_blocks_private_targets(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("MOATAZ_PROVIDER_NETWORK_POLICY", "hosted")
    with pytest.raises(HTTPException) as exc:
        _validate_network_target("http://127.0.0.1:11434")
    assert exc.value.status_code == 422


def test_local_provider_policy_allows_private_targets(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("MOATAZ_PROVIDER_NETWORK_POLICY", "local")
    _validate_network_target("http://127.0.0.1:11434")
