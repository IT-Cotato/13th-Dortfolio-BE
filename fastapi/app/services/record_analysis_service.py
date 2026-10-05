from uuid import UUID
from time import perf_counter

from fastapi import HTTPException, status
from google.genai import errors

from app.clients.gemini_record_analysis_client import GeminiRecordAnalysisClient
from app.core.config import get_settings
from app.core.ai_error import invalid_response_error, provider_error
from app.schemas.record_analysis import (
    RecordAnalysisRequest,
    RecordAnalysisResponse,
)


def analyze_record(
    request: RecordAnalysisRequest,
    request_id: UUID,
) -> RecordAnalysisResponse:
    settings = get_settings()

    if not settings.gemini_api_key:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI service is not configured.",
        )

    return analyze_record_with_gemini(request, request_id, settings)


def analyze_record_with_gemini(
    request: RecordAnalysisRequest,
    request_id: UUID,
    settings,
) -> RecordAnalysisResponse:
    client = GeminiRecordAnalysisClient(settings)
    started_at = perf_counter()

    try:
        summary, evidence_snippets, strength_tag_ids, usage = (
            client.analyze_record(request, request_id)
        )
    except errors.APIError as exception:
        raise provider_error(
            request_id, settings.gemini_generation_model, exception,
            int((perf_counter() - started_at) * 1000),
        ) from exception
    except ValueError as exception:
        raise invalid_response_error(
            request_id, settings.gemini_generation_model,
            int((perf_counter() - started_at) * 1000),
        ) from exception

    return RecordAnalysisResponse(
        summary=summary,
        evidenceSnippets=evidence_snippets,
        strengthTagIds=strength_tag_ids,
        usage=usage,
    )
