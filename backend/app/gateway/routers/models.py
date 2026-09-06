import logging

from fastapi import APIRouter, Depends, HTTPException, Request
from pydantic import BaseModel, Field

from app.gateway.authz import _AuthorizationUnavailable, _is_internal_caller, resolve_model_authorization
from app.gateway.deps import get_config, get_optional_user_from_request
from app.gateway.routers.providers import router as providers_router
from deerflow.authz.provider import AuthzDecision, AuthzRequest
from deerflow.config.app_config import AppConfig
from deerflow.config.model_config import ModelConfig
from deerflow.models.dynamic_registry import get_dynamic_model_config, list_dynamic_model_configs

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api", tags=["models"])
router.include_router(providers_router)


class ModelResponse(BaseModel):
    name: str = Field(..., description="Unique identifier for the model")
    model: str = Field(..., description="Actual provider model identifier")
    display_name: str | None = Field(None, description="Human-readable name")
    description: str | None = Field(None, description="Model description")
    supports_thinking: bool = Field(default=False, description="Whether model supports thinking mode")
    supports_reasoning_effort: bool = Field(default=False, description="Whether model supports reasoning effort")


class TokenUsageResponse(BaseModel):
    enabled: bool = Field(default=False, description="Whether token usage display is enabled")


class ModelsListResponse(BaseModel):
    models: list[ModelResponse]
    token_usage: TokenUsageResponse


def _all_model_configs(config: AppConfig) -> list[ModelConfig]:
    try:
        dynamic = list_dynamic_model_configs(include_credentials=False)
    except Exception:
        logger.warning("Dynamic provider registry could not be read; serving static models only", exc_info=True)
        dynamic = []
    static_names = {model.name for model in config.models}
    return [*config.models, *(model for model in dynamic if model.name not in static_names)]


async def _visible_models(request: Request, config: AppConfig) -> list[ModelConfig]:
    all_models = _all_model_configs(config)
    visible_models = all_models
    fail_closed = config.authorization.fail_closed
    user = await get_optional_user_from_request(request)
    if user is None:
        return visible_models
    try:
        provider, principal = resolve_model_authorization(user, is_internal=_is_internal_caller(request, user))
    except _AuthorizationUnavailable as exc:
        return [] if exc.fail_closed else visible_models
    if provider is None or principal is None:
        return visible_models
    try:
        allowed_names = provider.filter_resources(principal, "model", [model.name for model in all_models])
        if not isinstance(allowed_names, list) or any(not isinstance(name, str) for name in allowed_names):
            raise TypeError("AuthorizationProvider.filter_resources must return list[str]")
        allowed = set(allowed_names)
        return [model for model in all_models if model.name in allowed]
    except Exception:
        logger.warning("Authorization provider failed while filtering models", exc_info=True)
        return [] if fail_closed else visible_models


def _response(model: ModelConfig) -> ModelResponse:
    return ModelResponse(
        name=model.name,
        model=model.model,
        display_name=model.display_name,
        description=model.description,
        supports_thinking=model.supports_thinking,
        supports_reasoning_effort=model.supports_reasoning_effort,
    )


@router.get("/models", response_model=ModelsListResponse, summary="List All Models")
async def list_models(request: Request, config: AppConfig = Depends(get_config)) -> ModelsListResponse:
    models = [_response(model) for model in await _visible_models(request, config)]
    return ModelsListResponse(models=models, token_usage=TokenUsageResponse(enabled=config.token_usage.enabled))


@router.get("/models/{model_name}", response_model=ModelResponse, summary="Get Model Details")
async def get_model(model_name: str, request: Request, config: AppConfig = Depends(get_config)) -> ModelResponse:
    model = config.get_model_config(model_name)
    if model is None:
        try:
            model = get_dynamic_model_config(model_name, include_credentials=False)
        except Exception:
            logger.warning("Dynamic model lookup failed", exc_info=True)
            model = None
    if model is None:
        raise HTTPException(status_code=404, detail=f"Model '{model_name}' not found")

    fail_closed = config.authorization.fail_closed
    user = await get_optional_user_from_request(request)
    if user is not None:
        try:
            provider, principal = resolve_model_authorization(user, is_internal=_is_internal_caller(request, user))
        except _AuthorizationUnavailable:
            if fail_closed:
                raise HTTPException(status_code=403, detail=f"Model '{model_name}' is not available for your role")
        else:
            if provider is not None and principal is not None:
                try:
                    decision = provider.authorize(AuthzRequest(principal=principal, resource="model", action="use", target=model_name))
                    if not isinstance(decision, AuthzDecision):
                        raise TypeError("AuthorizationProvider.authorize must return AuthzDecision")
                    allowed = decision.allow
                except Exception:
                    logger.warning("Authorization provider failed while checking model:use for %s", model_name, exc_info=True)
                    allowed = not fail_closed
                if not allowed:
                    raise HTTPException(status_code=403, detail=f"Model '{model_name}' is not available for your role")
    return _response(model)
