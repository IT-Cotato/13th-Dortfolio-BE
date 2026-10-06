from google.genai import errors
from httpx import TimeoutException, TransportError

from app.core.ai_error import (
    configuration_error,
    provider_error,
    invalid_response_error,
    output_limit_error,
    transport_error,
)

from app.clients.gemini_insight_client import (
    GeminiInsightClient,
)
from app.core.config import get_settings
from app.core.generation import OutputTokenLimitError
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
        raise configuration_error()

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
                "competencyCount": len(request.competencies),
            },
        )
        raise provider_error(exception) from exception
    except TimeoutException as exception:
        raise transport_error(timeout=True) from exception
    except TransportError as exception:
        raise transport_error(timeout=False) from exception
    except OutputTokenLimitError as exception:
        raise output_limit_error() from exception
    except ValueError as exception:
        raise invalid_response_error() from exception
