import logging

from google.genai import errors


logger = logging.getLogger(__name__)


def log_gemini_api_error(
    *,
    operation: str,
    model: str,
    exception: errors.APIError,
) -> None:
    status = getattr(exception, "code", None)
    status = status if isinstance(status, int) and 400 <= status <= 599 else None
    logger.warning(
        "Gemini API request failed. operation=%s, model=%s, status=%s",
        operation,
        model,
        status,
    )
