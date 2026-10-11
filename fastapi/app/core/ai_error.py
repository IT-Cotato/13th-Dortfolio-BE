from uuid import UUID

from botocore.exceptions import (
    BotoCoreError,
    ClientError,
    ConnectTimeoutError,
    ReadTimeoutError,
)
from fastapi import Request
from fastapi.responses import JSONResponse

from app.schemas.ai_observability import AiErrorResponse


PROVIDER = "BEDROCK"


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


# Bedrock 호출에서 나올 수 있는 예외. ClientError는 서비스가 돌려준 오류,
# BotoCoreError는 타임아웃·연결 실패·자격 증명 누락처럼 응답을 받지 못한 경우다.
BEDROCK_ERRORS = (ClientError, BotoCoreError)


def bedrock_http_status(exception: Exception) -> int | None:
    if isinstance(exception, (ReadTimeoutError, ConnectTimeoutError)):
        return 504
    response = getattr(exception, "response", None)
    if not isinstance(response, dict):
        return None
    status = response.get("ResponseMetadata", {}).get("HTTPStatusCode")
    return status if isinstance(status, int) and 400 <= status <= 599 else None


def bedrock_error_code(exception: Exception) -> str | None:
    response = getattr(exception, "response", None)
    if isinstance(response, dict):
        return response.get("Error", {}).get("Code")
    return type(exception).__name__


def bedrock_retry_after(exception: Exception) -> str | None:
    response = getattr(exception, "response", None)
    if not isinstance(response, dict):
        return None
    headers = response.get("ResponseMetadata", {}).get("HTTPHeaders") or {}
    retry_after = headers.get("retry-after")
    return str(retry_after) if retry_after else None


def provider_error(request_id: UUID, model_id: str, exception: Exception, latency_ms: int) -> AiError:
    http_status = bedrock_http_status(exception) or 502
    status = "RATE_LIMITED" if http_status == 429 else (
        "TIMEOUT" if http_status in (408, 504) else "PROVIDER_ERROR"
    )
    return AiError(
        request_id=request_id,
        provider=PROVIDER,
        model_id=model_id,
        status=status,
        error_code=f"HTTP_{http_status}",
        latency_ms=latency_ms,
        http_status=http_status,
        retry_after=bedrock_retry_after(exception),
    )


def invalid_response_error(request_id: UUID, model_id: str, latency_ms: int) -> AiError:
    return AiError(
        request_id=request_id,
        provider=PROVIDER,
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
