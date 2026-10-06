from fastapi import HTTPException, Request
from fastapi.responses import JSONResponse


class AiError(HTTPException):
    def __init__(self, http_status: int, status: str, error_code: str, message: str, retry_after: str | None = None):
        headers = {"Retry-After": retry_after} if retry_after else None
        super().__init__(status_code=http_status, detail=message, headers=headers)
        self.status = status
        self.error_code = error_code


def provider_error(exception: Exception) -> AiError:
    code = getattr(exception, "code", None)
    http_status = code if isinstance(code, int) and 400 <= code <= 599 else 502
    provider_status = "RATE_LIMITED" if http_status == 429 else (
        "TIMEOUT" if http_status in (408, 504) else (
            "CONFIGURATION_ERROR" if http_status in (401, 403) else "PROVIDER_ERROR"
        )
    )
    response = getattr(exception, "response", None)
    headers = getattr(response, "headers", None)
    return AiError(
        http_status, provider_status, f"HTTP_{http_status}",
        "Gemini request failed.", headers.get("retry-after") if headers else None,
    )


def configuration_error() -> AiError:
    return AiError(503, "CONFIGURATION_ERROR", "API_KEY_MISSING", "GEMINI_API_KEY is required.")


def invalid_response_error() -> AiError:
    return AiError(422, "INVALID_RESPONSE", "SCHEMA_VALIDATION_FAILED", "Gemini response is invalid.")


def output_limit_error() -> AiError:
    return AiError(422, "OUTPUT_LIMIT", "OUTPUT_TOKEN_LIMIT", "Gemini generation reached the output token limit.")


def transport_error(timeout: bool) -> AiError:
    return AiError(
        504 if timeout else 502, "TIMEOUT" if timeout else "PROVIDER_ERROR",
        "TIMEOUT" if timeout else "CONNECTION_ERROR", "Gemini connection failed.",
    )


def handle_ai_error(_request: Request, exception: AiError) -> JSONResponse:
    return JSONResponse(
        status_code=exception.status_code,
        content={"status": exception.status, "errorCode": exception.error_code, "message": exception.detail},
        headers=exception.headers,
    )
