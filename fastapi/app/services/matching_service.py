from fastapi import HTTPException, status
from google.genai import errors

from app.clients.gemini_record_analysis_client import GeminiRecordAnalysisClient
from app.core.config import get_settings
from app.schemas.matching import QuestionEmbeddingRequest, QuestionEmbeddingResponse
from app.services.local_embedding_service import LOCAL_EMBEDDING_MODEL, create_local_embedding


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
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail="Gemini embedding request failed.",
            ) from exception
        except ValueError as exception:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail=str(exception),
            ) from exception

    if not settings.allow_local_analysis:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="GEMINI_API_KEY is required unless FASTAPI_ALLOW_LOCAL_ANALYSIS is enabled.",
        )
    return QuestionEmbeddingResponse(
        embeddingModel=LOCAL_EMBEDDING_MODEL,
        embedding=create_local_embedding(request.question),
    )
