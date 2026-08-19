import logging
from typing import Any

from google.genai import errors


logger = logging.getLogger(__name__)


def log_gemini_api_error(
    *,
    operation: str,
    model: str,
    exception: errors.APIError,
    context: dict[str, Any] | None = None,
) -> None:
    log_context = context or {}
    context_text = ", ".join(
        f"{key}={value}" for key, value in log_context.items()
    )

    logger.warning(
        "Gemini API request failed. operation=%s, model=%s, status=%s, message=%s%s",
        operation,
        model,
        getattr(exception, "code", None),
        str(exception),
        f", {context_text}" if context_text else "",
        exc_info=True,
    )
