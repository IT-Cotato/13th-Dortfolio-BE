from fastapi import HTTPException, status
from google.genai import errors

from app.clients.gemini_record_analysis_client import GeminiRecordAnalysisClient
from app.core.config import get_settings
from app.core.gemini_error_logging import log_gemini_api_error
from app.schemas.record_analysis import (
    RecordAnalysisRequest,
    RecordAnalysisResponse,
)


def analyze_record(request: RecordAnalysisRequest) -> RecordAnalysisResponse:
    settings = get_settings()
    if settings.gemini_api_key:
        return analyze_record_with_gemini(request, settings)
    raise HTTPException(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        detail="GEMINI_API_KEY is required.",
    )


def analyze_record_with_gemini(request: RecordAnalysisRequest, settings) -> RecordAnalysisResponse:
    client = GeminiRecordAnalysisClient(settings)
    try:
        summary, evidence_snippets, strength_tag_ids = client.analyze_record(request)
    except errors.APIError as exception:
        log_gemini_api_error(
            operation="record_analysis",
            model=settings.gemini_generation_model,
            exception=exception,
            context={"recordId": request.recordId},
        )
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Gemini analysis request failed.",
        ) from exception
    except ValueError as exception:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=str(exception),
        ) from exception

    return RecordAnalysisResponse(
        summary=summary,
        evidenceSnippets=evidence_snippets,
        strengthTagIds=strength_tag_ids,
    )
