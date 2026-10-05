from fastapi import HTTPException, status
from time import perf_counter
from uuid import UUID
from google.genai import errors

from app.clients.gemini_insight_client import (
    GeminiInsightClient,
)
from app.core.config import get_settings
from app.core.ai_error import invalid_response_error, provider_error
from app.schemas.insight import (
    InsightRecommendationRequest,
    InsightRecommendationResponse,
)


def generate_insight_recommendation(
    request: InsightRecommendationRequest,
    request_id: UUID,
) -> InsightRecommendationResponse:
    settings = get_settings()

    if not settings.gemini_api_key:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="GEMINI_API_KEY is required.",
        )

    client = GeminiInsightClient(settings)
    started_at = perf_counter()

    try:
        return client.generate_recommendation(request, request_id)
    except errors.APIError as exception:
        raise provider_error(
            request_id, settings.gemini_generation_model, exception,
            int((perf_counter() - started_at) * 1000),
        ) from exception
    except ValueError as exception:
        raise invalid_response_error(
            request_id, settings.gemini_generation_model,
            int((perf_counter() - started_at) * 1000),
        ) from exception


def gemini_error_status(exception: errors.APIError) -> int:
    """Preserve actionable Gemini statuses for the Spring retry policy."""
    code = getattr(exception, "code", None)

    if isinstance(code, int) and 400 <= code <= 599:
        return code

    return status.HTTP_502_BAD_GATEWAY


def gemini_retry_headers(
    exception: errors.APIError,
) -> dict[str, str] | None:
    response = getattr(exception, "response", None)
    response_headers = getattr(response, "headers", None)

    if response_headers is None:
        return None

    retry_after = response_headers.get("retry-after")

    if not retry_after:
        return None

    return {"Retry-After": str(retry_after)}
