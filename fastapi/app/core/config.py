from functools import lru_cache
from os import getenv


def positive_integer(name: str, default: str) -> int:
    value = int(getenv(name, default))
    if value <= 0:
        raise ValueError(f"{name} must be positive.")
    return value


def thinking_level(name: str) -> str | None:
    value = getenv(name, "").strip().lower() or None
    if value not in (None, "minimal", "low", "medium", "high"):
        raise ValueError(f"{name} must be minimal, low, medium, high, or empty.")
    return value


class Settings:
    record_analysis_max_output_tokens: int = positive_integer("RECORD_ANALYSIS_MAX_OUTPUT_TOKENS", "8192")
    insight_recommendation_max_output_tokens: int = positive_integer("INSIGHT_RECOMMENDATION_MAX_OUTPUT_TOKENS", "8192")
    record_analysis_thinking_level: str | None = thinking_level("RECORD_ANALYSIS_THINKING_LEVEL")
    insight_recommendation_thinking_level: str | None = thinking_level("INSIGHT_RECOMMENDATION_THINKING_LEVEL")
    gemini_api_key: str | None = getenv("GEMINI_API_KEY") or None
    gemini_generation_model: str = getenv("GEMINI_GENERATION_MODEL", "gemini-3.6-flash")
    gemini_embedding_model: str = getenv("GEMINI_EMBEDDING_MODEL", "gemini-embedding-2")
    gemini_http_timeout_seconds: float = float(getenv("GEMINI_HTTP_TIMEOUT_SECONDS", "25"))


@lru_cache
def get_settings() -> Settings:
    return Settings()
