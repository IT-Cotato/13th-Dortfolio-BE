from typing import Literal

from pydantic import BaseModel
from app.schemas.ai_observability import AiUsage


class QuestionEmbeddingRequest(BaseModel):
    question: str
    # Cohere 임베딩은 저장용 문서와 검색 질의를 구분한다.
    # 기록·강점 태그·직무 역량처럼 DB에 저장하는 텍스트는 search_document,
    # 저장된 기록을 찾기 위한 질문은 search_query로 보낸다.
    inputType: Literal["search_document", "search_query"] = "search_document"


class QuestionEmbeddingResponse(BaseModel):
    embeddingModel: str
    embedding: list[float]
    usage: AiUsage
