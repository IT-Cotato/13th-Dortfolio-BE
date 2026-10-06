import logging

from google.genai import types


logger = logging.getLogger(__name__)


class OutputTokenLimitError(ValueError):
    pass


def generation_config(settings, feature: str) -> dict:
    level = getattr(settings, f"{feature}_thinking_level")
    config = {
        "max_output_tokens": getattr(settings, f"{feature}_max_output_tokens"),
        "http_options": types.HttpOptions(timeout=int(settings.gemini_http_timeout_seconds * 1000)),
    }
    if level is not None:
        config["thinking_config"] = types.ThinkingConfig(thinking_level=level)
    return config


def check_generation_response(response, feature: str) -> None:
    usage = response.usage_metadata
    finish_reason = response.candidates[0].finish_reason if response.candidates else None
    logger.info(
        "Gemini generation completed. feature=%s, finishReason=%s, inputTokens=%s, "
        "outputTokens=%s, thinkingTokens=%s",
        feature,
        finish_reason,
        usage.prompt_token_count if usage else None,
        usage.candidates_token_count if usage else None,
        usage.thoughts_token_count if usage else None,
    )
    if finish_reason == types.FinishReason.MAX_TOKENS:
        raise OutputTokenLimitError("Gemini generation reached the output token limit.")
