from google.genai import errors
from httpx import TimeoutException, TransportError

from app.core.ai_error import (
    configuration_error,
    provider_error,
    invalid_response_error,
    transport_error,
)

from app.clients.gemini_record_analysis_client import GeminiRecordAnalysisClient
from app.core.config import get_settings
from app.core.gemini_error_logging import log_gemini_api_error
from app.schemas.matching import QuestionEmbeddingRequest, QuestionEmbeddingResponse


def embed_question(request: QuestionEmbeddingRequest) -> QuestionEmbeddingResponse:
    settings = get_settings()
    if settings.gemini_api_key:
        client = GeminiRecordAnalysisClient(settings)
        try:
            return QuestionEmbeddingResponse(
                embeddingModel=settings.gemini_embedding_model,
                embedding=client.embed_record(request.question),
            )
        except errors.APIError as exception:
            log_gemini_api_error(
                operation="question_embedding",
                model=settings.gemini_embedding_model,
                exception=exception,
            )
            raise provider_error(exception) from exception
        except TimeoutException as exception:
            raise transport_error(timeout=True) from exception
        except TransportError as exception:
            raise transport_error(timeout=False) from exception
        except ValueError as exception:
            raise invalid_response_error() from exception

    raise configuration_error()
