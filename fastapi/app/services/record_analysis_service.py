from uuid import UUID
from time import perf_counter

from fastapi import HTTPException, status

from app.clients.bedrock_record_analysis_client import BedrockRecordAnalysisClient
from app.clients.bedrock_runtime import bedrock_credentials_available
from app.core.bedrock_error_logging import log_bedrock_api_error
from app.core.config import get_settings
from app.core.ai_error import BEDROCK_ERRORS, invalid_response_error, provider_error
from app.core.generation import OutputTokenLimitError
from app.schemas.record_analysis import (
    RecordAnalysisRequest,
    RecordAnalysisResponse,
)


def analyze_record(
    request: RecordAnalysisRequest,
    request_id: UUID,
) -> RecordAnalysisResponse:
    settings = get_settings()

    if not bedrock_credentials_available():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI service is not configured.",
        )

    return analyze_record_with_bedrock(request, request_id, settings)


def analyze_record_with_bedrock(
    request: RecordAnalysisRequest,
    request_id: UUID,
    settings,
) -> RecordAnalysisResponse:
    client = BedrockRecordAnalysisClient(settings)
    started_at = perf_counter()

    try:
        return client.analyze_record(request, request_id)
    except BEDROCK_ERRORS as exception:
        log_bedrock_api_error(
            operation="record_analysis",
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
