from pydantic import BaseModel
from app.schemas.ai_observability import AiUsage


class QuestionEmbeddingRequest(BaseModel):
    question: str


class QuestionEmbeddingResponse(BaseModel):
    embeddingModel: str
    embedding: list[float]
    usage: AiUsage
