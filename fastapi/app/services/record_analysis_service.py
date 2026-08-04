from fastapi import HTTPException, status
from google.genai import errors

from app.clients.gemini_record_analysis_client import GeminiRecordAnalysisClient
from app.core.config import get_settings
from app.schemas.record_analysis import (
    AnalyzedCompetencyTagResponse,
    RecordAnalysisRequest,
    RecordAnalysisResponse,
)
from app.services.local_embedding_service import LOCAL_EMBEDDING_MODEL, create_local_embedding


def analyze_record(request: RecordAnalysisRequest) -> RecordAnalysisResponse:
    settings = get_settings()
    if settings.gemini_api_key:
        return analyze_record_with_gemini(request, settings)
    if not settings.allow_local_analysis:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="GEMINI_API_KEY is required unless FASTAPI_ALLOW_LOCAL_ANALYSIS is enabled.",
        )
    return analyze_record_locally(request)


def analyze_record_with_gemini(request: RecordAnalysisRequest, settings) -> RecordAnalysisResponse:
    client = GeminiRecordAnalysisClient(settings)
    answer_texts = [answer.answerText.strip() for answer in request.answers if answer.answerText.strip()]
    source_text = build_embedding_source_text(request, answer_texts)
    try:
        summary, evidence_snippets, competency_tags = client.analyze_record(request)
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
        competencyTags=competency_tags,
        embeddingModel=settings.gemini_embedding_model,
        embedding=embedding,
    )


def analyze_record_locally(request: RecordAnalysisRequest) -> RecordAnalysisResponse:
    answer_texts = [answer.answerText.strip() for answer in request.answers if answer.answerText.strip()]
    memo_texts = build_memo_texts(request)
    question_texts = [answer.questionText.strip() for answer in request.answers if answer.questionText.strip()]
    evidence_snippets = (answer_texts[:3] or memo_texts[:3] or question_texts[:3] or [request.title])
    source_text = build_embedding_source_text(request, answer_texts)
    summary_source = answer_texts[0] if answer_texts else (memo_texts[0] if memo_texts else request.title)

    return RecordAnalysisResponse(
        summary=f"{request.title}: {summary_source[:120]}",
        evidenceSnippets=evidence_snippets,
        competencyTags=select_competency_tags(request),
        embeddingModel=LOCAL_EMBEDDING_MODEL,
        embedding=create_local_embedding(source_text),
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


def select_competency_tags(request: RecordAnalysisRequest) -> list[AnalyzedCompetencyTagResponse]:
    return [
        AnalyzedCompetencyTagResponse(
            competencyTagId=candidate.id,
            score=round(max(0.5, 1.0 - index * 0.1), 2),
        )
        for index, candidate in enumerate(request.competencyTagCandidates[:3])
    ]
