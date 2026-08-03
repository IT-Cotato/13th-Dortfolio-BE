from pydantic import BaseModel


class QuestionEmbeddingRequest(BaseModel):
    question: str


class QuestionEmbeddingResponse(BaseModel):
    embeddingModel: str
    embedding: list[float]
