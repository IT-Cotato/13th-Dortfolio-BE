from google.genai import errors
from httpx import TimeoutException, TransportError

from app.core.ai_error import (
    configuration_error,
    provider_error,
    invalid_response_error,
    output_limit_error,
    transport_error,
)

from app.clients.gemini_record_analysis_client import GeminiRecordAnalysisClient
from app.core.config import get_settings
from app.core.generation import OutputTokenLimitError
from app.core.gemini_error_logging import log_gemini_api_error
from app.schemas.record_analysis import (
    RecordAnalysisRequest,
    RecordAnalysisResponse,
)


def analyze_record(request: RecordAnalysisRequest) -> RecordAnalysisResponse:
    settings = get_settings()
    if settings.gemini_api_key:
        return analyze_record_with_gemini(request, settings)
    raise configuration_error()


def analyze_record_with_gemini(request: RecordAnalysisRequest, settings) -> RecordAnalysisResponse:
    client = GeminiRecordAnalysisClient(settings)
    try:
        return client.analyze_record(request)
    except errors.APIError as exception:
        log_gemini_api_error(
            operation="record_analysis",
            model=settings.gemini_generation_model,
            exception=exception,
            context={"recordId": request.recordId},
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
