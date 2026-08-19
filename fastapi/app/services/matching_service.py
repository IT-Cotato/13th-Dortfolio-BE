from fastapi import HTTPException, status
from google.genai import errors

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
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail="Gemini embedding request failed.",
            ) from exception
        except ValueError as exception:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail=str(exception),
            ) from exception

    raise HTTPException(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        detail="GEMINI_API_KEY is required.",
    )
