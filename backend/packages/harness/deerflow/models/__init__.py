from __future__ import annotations

from langchain.chat_models import BaseChatModel
from langchain_openai import ChatOpenAI

from deerflow.tracing import build_tracing_callbacks

from .dynamic_registry import (
    get_default_dynamic_model_name,
    get_dynamic_model_config,
    is_dynamic_model_name,
)
from .factory import create_chat_model as _create_static_chat_model


def _create_dynamic_chat_model(
    name: str,
    *,
    thinking_enabled: bool = False,
    attach_tracing: bool = True,
    model_overrides: dict | None = None,
    **kwargs,
) -> BaseChatModel:
    config = get_dynamic_model_config(name, include_credentials=True)
    if config is None:
        raise ValueError(f"Dynamic model {name} not found")

    settings = config.model_dump(
        exclude_none=True,
        exclude={
            "use",
            "name",
            "display_name",
            "description",
            "supports_thinking",
            "supports_reasoning_effort",
            "when_thinking_enabled",
            "when_thinking_disabled",
            "thinking",
            "supports_vision",
            "context_window",
            "pricing",
        },
    )
    if model_overrides:
        settings.update({key: value for key, value in model_overrides.items() if value is not None})

    explicit_effort = kwargs.pop("reasoning_effort", None)
    if config.supports_reasoning_effort:
        if explicit_effort:
            settings["reasoning_effort"] = explicit_effort
        elif thinking_enabled:
            settings.setdefault("reasoning_effort", "medium")
    else:
        settings.pop("reasoning_effort", None)

    model = ChatOpenAI(**kwargs, **settings)
    if config.context_window:
        inferred_profile = getattr(model, "profile", None)
        model.profile = {**(inferred_profile or {}), "max_input_tokens": config.context_window}
    if attach_tracing:
        callbacks = build_tracing_callbacks()
        if callbacks:
            model.callbacks = [*(model.callbacks or []), *callbacks]
    return model


def create_chat_model(
    name: str | None = None,
    thinking_enabled: bool = False,
    *,
    app_config=None,
    attach_tracing: bool = True,
    model_overrides: dict | None = None,
    **kwargs,
) -> BaseChatModel:
    """Create either an upstream YAML model or a moataz ai dynamic BYOK model.

    Static DeerFlow behavior is delegated unchanged to the original factory.
    Dynamic names are resolved from the encrypted registry.  A configured
    dynamic default is also honored when callers omit ``name``.
    """
    dynamic_name = name if is_dynamic_model_name(name) else None
    if name is None:
        dynamic_name = get_default_dynamic_model_name()
    if dynamic_name:
        return _create_dynamic_chat_model(
            dynamic_name,
            thinking_enabled=thinking_enabled,
            attach_tracing=attach_tracing,
            model_overrides=model_overrides,
            **kwargs,
        )
    return _create_static_chat_model(
        name,
        thinking_enabled,
        app_config=app_config,
        attach_tracing=attach_tracing,
        model_overrides=model_overrides,
        **kwargs,
    )


__all__ = ["create_chat_model"]
