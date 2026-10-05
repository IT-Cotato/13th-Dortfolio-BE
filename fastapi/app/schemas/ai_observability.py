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