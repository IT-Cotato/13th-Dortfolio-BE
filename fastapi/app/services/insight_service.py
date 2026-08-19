from fastapi import HTTPException, status
from google.genai import errors

from app.clients.gemini_insight_client import (
    GeminiInsightClient,
)
from app.core.config import get_settings
from app.core.gemini_error_logging import log_gemini_api_error
from app.schemas.insight import (
    InsightRecommendationRequest,
    InsightRecommendationResponse,
)


def generate_insight_recommendation(
    request: InsightRecommendationRequest,
) -> InsightRecommendationResponse:
    settings = get_settings()

    if not settings.gemini_api_key:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="GEMINI_API_KEY is required.",
        )

    client = GeminiInsightClient(settings)

    try:
        return client.generate_recommendation(request)
    except errors.APIError as exception:
        log_gemini_api_error(
            operation="insight_recommendation",
            model=settings.gemini_generation_model,
            exception=exception,
            context={
                "jobId": request.jobId,
                "jobCompetencyId": request.jobCompetencyId,
            },
        )
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Gemini recommendation request failed.",
        ) from exception
    except ValueError as exception:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=str(exception),
        ) from exception
