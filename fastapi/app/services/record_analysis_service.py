from hashlib import sha256

from fastapi import HTTPException, status

from app.clients.gemini_record_analysis_client import GeminiRecordAnalysisClient
from app.core.config import get_settings
from app.schemas.record_analysis import (
    AnalyzedCompetencyTagResponse,
    RecordAnalysisRequest,
    RecordAnalysisResponse,
)

LOCAL_EMBEDDING_MODEL = "dortfolio-local-hash-v1"
LOCAL_EMBEDDING_DIMENSIONS = 3072


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
    summary, evidence_snippets, competency_tags = client.analyze_record(request)

    return RecordAnalysisResponse(
        summary=summary,
        evidenceSnippets=evidence_snippets,
        competencyTags=competency_tags,
        embeddingModel=settings.gemini_embedding_model,
        embedding=client.embed_record(source_text),
    )


def analyze_record_locally(request: RecordAnalysisRequest) -> RecordAnalysisResponse:
    answer_texts = [answer.answerText.strip() for answer in request.answers if answer.answerText.strip()]
    evidence_snippets = answer_texts[:3] or [request.title]
    source_text = build_embedding_source_text(request, answer_texts)
    summary_source = answer_texts[0] if answer_texts else request.title

    return RecordAnalysisResponse(
        summary=f"{request.title}: {summary_source[:120]}",
        evidenceSnippets=evidence_snippets,
        competencyTags=select_competency_tags(request),
        embeddingModel=LOCAL_EMBEDDING_MODEL,
        embedding=create_local_embedding(source_text),
    )


def build_embedding_source_text(request: RecordAnalysisRequest, answer_texts: list[str]) -> str:
    return " ".join([
        request.activity.title,
        request.activity.description or "",
        request.title,
        request.template.title,
        " ".join(answer_texts),
    ]).strip()


def select_competency_tags(request: RecordAnalysisRequest) -> list[AnalyzedCompetencyTagResponse]:
    return [
        AnalyzedCompetencyTagResponse(
            competencyTagId=candidate.id,
            score=round(max(0.5, 1.0 - index * 0.1), 2),
        )
        for index, candidate in enumerate(request.competencyTagCandidates[:3])
    ]


def create_local_embedding(source_text: str) -> list[float]:
    values: list[float] = []
    seed = source_text.encode("utf-8")
    round_index = 0

    while len(values) < LOCAL_EMBEDDING_DIMENSIONS:
        digest = sha256(seed + round_index.to_bytes(4, "big")).digest()
        values.extend(((byte / 255.0) * 2.0) - 1.0 for byte in digest)
        round_index += 1

    return values[:LOCAL_EMBEDDING_DIMENSIONS]
