from uuid import UUID
from time import perf_counter

from fastapi import HTTPException, status
from google.genai import errors

from app.clients.gemini_record_analysis_client import GeminiRecordAnalysisClient
from app.core.config import get_settings
from app.core.ai_error import invalid_response_error, provider_error
from app.schemas.matching import QuestionEmbeddingRequest, QuestionEmbeddingResponse


def embed_question(
    request: QuestionEmbeddingRequest,
    request_id: UUID,
) -> QuestionEmbeddingResponse:
    settings = get_settings()

    if not settings.gemini_api_key:
        raise HTTPException(
            status_code=503,
            detail="AI service is not configured.",
        )

    client = GeminiRecordAnalysisClient(settings)
    started_at = perf_counter()
    try:
        embedding, usage = client.embed_record(
            request.question,
            request_id,
        )
        return QuestionEmbeddingResponse(
            embeddingModel=usage.modelId,
            embedding=embedding,
            usage=usage,
        )
    except errors.APIError as exception:
        raise provider_error(
            request_id, settings.gemini_embedding_model, exception,
            int((perf_counter() - started_at) * 1000),
        ) from exception
    except ValueError as exception:
        raise invalid_response_error(
            request_id, settings.gemini_embedding_model,
            int((perf_counter() - started_at) * 1000),
        ) from exception
