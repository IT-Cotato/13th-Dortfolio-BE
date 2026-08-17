from fastapi import HTTPException, status
from google.genai import errors

from app.clients.gemini_record_analysis_client import GeminiRecordAnalysisClient
from app.core.config import get_settings
from app.schemas.record_analysis import (
    RecordAnalysisRequest,
    RecordAnalysisResponse,
)


def analyze_record(request: RecordAnalysisRequest) -> RecordAnalysisResponse:
    settings = get_settings()
    if settings.gemini_api_key:
        return analyze_record_with_gemini(request, settings)
    raise HTTPException(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        detail="GEMINI_API_KEY is required.",
    )


def analyze_record_with_gemini(request: RecordAnalysisRequest, settings) -> RecordAnalysisResponse:
    client = GeminiRecordAnalysisClient(settings)
    answer_texts = [answer.answerText.strip() for answer in request.answers if answer.answerText.strip()]
    source_text = build_embedding_source_text(request, answer_texts)
    try:
        summary, evidence_snippets, strength_tags = client.analyze_record(request)
        embedding = client.embed_record(source_text)
    except errors.APIError as exception:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Gemini analysis request failed.",
        ) from exception
    except ValueError as exception:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=str(exception),
        ) from exception

    return RecordAnalysisResponse(
        summary=summary,
        evidenceSnippets=evidence_snippets,
        strengthTags=strength_tags,
        embeddingModel=settings.gemini_embedding_model,
        embedding=embedding,
    )


def build_embedding_source_text(request: RecordAnalysisRequest, answer_texts: list[str]) -> str:
    question_texts = [answer.questionText.strip() for answer in request.answers if answer.questionText.strip()]
    memo_texts = build_memo_texts(request)
    return " ".join([
        request.activity.title,
        request.activity.description or "",
        request.title,
        request.template.title,
        " ".join(question_texts),
        " ".join(answer_texts),
        " ".join(memo_texts),
    ]).strip()


def build_memo_texts(request: RecordAnalysisRequest) -> list[str]:
    return [
        " ".join(part for part in [memo.title, memo.content] if part and part.strip()).strip()
        for memo in request.memos
        if (memo.title and memo.title.strip()) or memo.content.strip()
    ]
