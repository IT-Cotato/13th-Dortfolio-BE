from fastapi import HTTPException, status
from time import perf_counter
from uuid import UUID

from app.clients.bedrock_insight_client import (
    BedrockInsightClient,
)
from app.clients.bedrock_runtime import bedrock_credentials_available
from app.core.bedrock_error_logging import log_bedrock_api_error
from app.core.config import get_settings
from app.core.ai_error import BEDROCK_ERRORS, invalid_response_error, provider_error
from app.core.generation import OutputTokenLimitError
from app.schemas.insight import (
    InsightRecommendationRequest,
    InsightRecommendationResponse,
)


def generate_insight_recommendation(
    request: InsightRecommendationRequest,
    request_id: UUID,
) -> InsightRecommendationResponse:
    settings = get_settings()

    if not bedrock_credentials_available():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI service is not configured.",
        )

    client = BedrockInsightClient(settings)
    started_at = perf_counter()

    try:
        return client.generate_recommendation(request, request_id)
    except BEDROCK_ERRORS as exception:
        log_bedrock_api_error(
            operation="insight_recommendation",
            model=settings.bedrock_generation_model_id,
            exception=exception,
        )
        raise provider_error(
            request_id, settings.bedrock_generation_model_id, exception,
            int((perf_counter() - started_at) * 1000),
        ) from exception
    except OutputTokenLimitError as exception:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=str(exception),
        ) from exception
    except ValueError as exception:
        raise invalid_response_error(
            request_id, settings.bedrock_generation_model_id,
            int((perf_counter() - started_at) * 1000),
        ) from exception
