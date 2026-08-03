from hashlib import sha256

from fastapi import HTTPException, status
from google.genai import errors

from app.clients.gemini_record_analysis_client import GeminiRecordAnalysisClient
from app.core.config import get_settings
from app.schemas.matching import QuestionEmbeddingRequest, QuestionEmbeddingResponse
from app.services.record_analysis_service import LOCAL_EMBEDDING_DIMENSIONS, LOCAL_EMBEDDING_MODEL


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


def create_local_embedding(source_text: str) -> list[float]:
    values: list[float] = []
    seed = source_text.encode("utf-8")
    round_index = 0

    while len(values) < LOCAL_EMBEDDING_DIMENSIONS:
        digest = sha256(seed + round_index.to_bytes(4, "big")).digest()
        values.extend(((byte / 255.0) * 2.0) - 1.0 for byte in digest)
        round_index += 1

    return values[:LOCAL_EMBEDDING_DIMENSIONS]
