from uuid import UUID
from time import perf_counter

from fastapi import HTTPException

from app.clients.bedrock_embedding_client import BedrockEmbeddingClient
from app.clients.bedrock_runtime import bedrock_credentials_available
from app.core.bedrock_error_logging import log_bedrock_api_error
from app.core.config import get_settings
from app.core.ai_error import BEDROCK_ERRORS, invalid_response_error, provider_error
from app.schemas.matching import QuestionEmbeddingRequest, QuestionEmbeddingResponse


def embed_question(
    request: QuestionEmbeddingRequest,
    request_id: UUID,
) -> QuestionEmbeddingResponse:
    settings = get_settings()

    if not bedrock_credentials_available():
        raise HTTPException(
            status_code=503,
            detail="AI service is not configured.",
        )

    client = BedrockEmbeddingClient(settings)
    started_at = perf_counter()
    try:
        embedding, usage = client.embed_text(
            request.question,
            request.inputType,
            request_id,
        )
        return QuestionEmbeddingResponse(
            embeddingModel=settings.embedding_model_name,
            embedding=embedding,
            usage=usage,
        )
    except BEDROCK_ERRORS as exception:
        log_bedrock_api_error(
            operation="embedding",
            model=settings.bedrock_embedding_model_id,
            exception=exception,
        )
        raise provider_error(
            request_id, settings.bedrock_embedding_model_id, exception,
            int((perf_counter() - started_at) * 1000),
        ) from exception
    except ValueError as exception:
        raise invalid_response_error(
            request_id, settings.bedrock_embedding_model_id,
            int((perf_counter() - started_at) * 1000),
        ) from exception
