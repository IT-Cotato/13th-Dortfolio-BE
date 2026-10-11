import logging


logger = logging.getLogger(__name__)


class OutputTokenLimitError(ValueError):
    pass


def inference_config(settings, feature: str) -> dict:
    return {"maxTokens": getattr(settings, f"{feature}_max_output_tokens")}


def check_generation_response(response: dict, feature: str) -> None:
    usage = response.get("usage") or {}
    stop_reason = response.get("stopReason")
    logger.info(
        "Bedrock generation completed. feature=%s, stopReason=%s, inputTokens=%s, outputTokens=%s",
        feature,
        stop_reason,
        usage.get("inputTokens"),
        usage.get("outputTokens"),
    )
    if stop_reason == "max_tokens":
        raise OutputTokenLimitError("Bedrock generation reached the output token limit.")
