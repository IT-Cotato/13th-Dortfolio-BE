from functools import lru_cache
from os import getenv


class Settings:
    gemini_api_key: str | None = getenv("GEMINI_API_KEY") or None
    gemini_generation_model: str = getenv("GEMINI_GENERATION_MODEL", "gemini-3.6-flash")
    gemini_embedding_model: str = getenv("GEMINI_EMBEDDING_MODEL", "gemini-embedding-2")
    gemini_http_timeout_seconds: float = float(getenv("GEMINI_HTTP_TIMEOUT_SECONDS", "25"))


@lru_cache
def get_settings() -> Settings:
    return Settings()
