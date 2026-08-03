from fastapi import APIRouter

from app.schemas.matching import (
    QuestionEmbeddingRequest,
    QuestionEmbeddingResponse,
)
from app.services.matching_service import embed_question

router = APIRouter(prefix="/ai/matching", tags=["record-matching"])


@router.post("/question-embedding", response_model=QuestionEmbeddingResponse)
def embed_question_endpoint(request: QuestionEmbeddingRequest):
    return embed_question(request)
