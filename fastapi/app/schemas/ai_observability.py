from typing import Literal
from uuid import UUID

from pydantic import BaseModel

class AiUsage(BaseModel):
    requestId: UUID
    provider: str
    modelId: str
    inputTokens: int | None = None
    outputTokens: int | None = None
    latencyMs: int


def billable_output_tokens(metadata: object | None) -> int | None:
    """Gemini bills both candidate and thinking tokens at the output rate."""
    candidates = getattr(metadata, "candidates_token_count", None)
    thinking = getattr(metadata, "thoughts_token_count", None)
    if candidates is None and thinking is None:
        return None
    return (candidates or 0) + (thinking or 0)

class AiErrorResponse(BaseModel):
    requestId: UUID
    provider: str
    modelId: str
    status: Literal[
        "TIMEOUT",
        "RATE_LIMITED",
        "PROVIDER_ERROR",
        "INVALID_RESPONSE",
        "UNKNOWN_ERROR",
    ]
    errorCode: str | None = None
    latencyMs: int
