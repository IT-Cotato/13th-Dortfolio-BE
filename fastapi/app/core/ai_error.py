from uuid import UUID

from fastapi import Request
from fastapi.responses import JSONResponse

from app.schemas.ai_observability import AiErrorResponse


class AiError(Exception):
    def __init__(
        self,
        *,
        request_id: UUID,
        provider: str,
        model_id: str,
        status: str,
        error_code: str,
        latency_ms: int,
        http_status: int,
        retry_after: str | None = None,
    ):
        self.request_id = request_id
        self.provider = provider
        self.model_id = model_id
        self.status = status
        self.error_code = error_code
        self.latency_ms = latency_ms
        self.http_status = http_status
        self.retry_after = retry_after


def provider_error(request_id: UUID, model_id: str, exception: Exception, latency_ms: int) -> AiError:
    code = getattr(exception, "code", None)
    http_status = code if isinstance(code, int) and 400 <= code <= 599 else 502
    status = "RATE_LIMITED" if http_status == 429 else (
        "TIMEOUT" if http_status in (408, 504) else "PROVIDER_ERROR"
    )
    response = getattr(exception, "response", None)
    headers = getattr(response, "headers", None)
    retry_after = headers.get("retry-after") if headers else None
    return AiError(
        request_id=request_id,
        provider="GEMINI",
        model_id=model_id,
        status=status,
        error_code=f"HTTP_{http_status}",
        latency_ms=latency_ms,
        http_status=http_status,
        retry_after=retry_after,
    )


def invalid_response_error(request_id: UUID, model_id: str, latency_ms: int) -> AiError:
    return AiError(
        request_id=request_id,
        provider="GEMINI",
        model_id=model_id,
        status="INVALID_RESPONSE",
        error_code="SCHEMA_VALIDATION_FAILED",
        latency_ms=latency_ms,
        http_status=502,
    )


def handle_ai_error(_request: Request, exception: AiError) -> JSONResponse:
    body = AiErrorResponse(
        requestId=exception.request_id,
        provider=exception.provider,
        modelId=exception.model_id,
        status=exception.status,
        errorCode=exception.error_code,
        latencyMs=exception.latency_ms,
    )
    headers = {"Retry-After": exception.retry_after} if exception.retry_after else None
    return JSONResponse(
        status_code=exception.http_status,
        content=body.model_dump(mode="json", exclude_none=True),
        headers=headers,
    )
