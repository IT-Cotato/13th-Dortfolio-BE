import json
from uuid import UUID

from google import genai
from google.genai import types
from pydantic import ValidationError

from app.core.config import Settings
from app.schemas.record_analysis import (
    AnalyzedCompetencyTagResponse,
    RecordAnalysisRequest,
)


class GeminiRecordAnalysisClient:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.client = genai.Client(
            api_key=settings.gemini_api_key,
            http_options=types.HttpOptions(timeout=int(settings.gemini_http_timeout_seconds * 1000)),
        )

    def analyze_record(self, request: RecordAnalysisRequest) -> tuple[str, list[str], list[AnalyzedCompetencyTagResponse]]:
        prompt = build_analysis_prompt(request)
        response = self.client.models.generate_content(
            model=self.settings.gemini_generation_model,
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                http_options=types.HttpOptions(timeout=int(self.settings.gemini_http_timeout_seconds * 1000)),
            ),
        )
        payload = json.loads(response.text or "{}")
        return parse_analysis_payload(payload, request)

    def embed_record(self, text: str) -> list[float]:
        result = self.client.models.embed_content(
            model=self.settings.gemini_embedding_model,
            contents=text,
        )
        return extract_embedding_values(result)


def build_analysis_prompt(request: RecordAnalysisRequest) -> str:
    answers = "\n".join(
        f"- 질문: {answer.questionText}\n  답변: {answer.answerText}"
        for answer in request.answers
    )
    memos = "\n".join(
        f"- 제목: {memo.title or ''}\n  내용: {memo.content}"
        for memo in request.memos
    )
    competency_tags = "\n".join(
        f"- id: {candidate.id}, name: {candidate.name}, description: {candidate.description or ''}"
        for candidate in request.competencyTagCandidates
    )

    return f"""
아래 기록을 분석해서 JSON만 반환해 주세요.

규칙:
- summary는 한국어 1문장으로 작성합니다.
- evidenceSnippets는 답변/메모 원문에서 핵심 근거 문장만 1~5개 추출합니다.
- competencyTags는 반드시 후보군 id 중에서만 선택합니다.
- score는 0.0 이상 1.0 이하 숫자입니다.
- 새로운 태그 id를 만들지 않습니다.

응답 JSON 형식:
{{
  "summary": "string",
  "evidenceSnippets": ["string"],
  "competencyTags": [
    {{"competencyTagId": "uuid", "score": 0.9}}
  ]
}}

기록:
- recordId: {request.recordId}
- title: {request.title}
- activityTitle: {request.activity.title}
- activityDescription: {request.activity.description or ""}
- templateTitle: {request.template.title}

답변:
{answers}

메모:
{memos}

강점 태그 후보군:
{competency_tags}
""".strip()


def parse_analysis_payload(
    payload: dict,
    request: RecordAnalysisRequest,
) -> tuple[str, list[str], list[AnalyzedCompetencyTagResponse]]:
    if not isinstance(payload, dict):
        raise ValueError("Gemini analysis response must be a JSON object.")
    required_fields = {"summary", "evidenceSnippets", "competencyTags"}
    if not required_fields.issubset(payload):
        raise ValueError("Gemini analysis response is missing required fields.")

    summary = payload["summary"]
    evidence_snippets = payload["evidenceSnippets"]
    raw_competency_tags = payload["competencyTags"]
    if not isinstance(summary, str):
        raise ValueError("Gemini analysis summary must be a string.")
    if not isinstance(evidence_snippets, list) or not all(isinstance(item, str) for item in evidence_snippets):
        raise ValueError("Gemini analysis evidenceSnippets must be a string array.")
    if not isinstance(raw_competency_tags, list):
        raise ValueError("Gemini analysis competencyTags must be an array.")

    candidate_ids = {candidate.id for candidate in request.competencyTagCandidates}
    seen_ids: set[UUID] = set()
    competency_tags = [
        tag
        for tag in (
            parse_competency_tag(tag_payload, candidate_ids, seen_ids)
            for tag_payload in raw_competency_tags
        )
        if tag is not None
    ]
    return summary, evidence_snippets, competency_tags


def parse_competency_tag(
    payload: object,
    candidate_ids: set[UUID],
    seen_ids: set[UUID],
) -> AnalyzedCompetencyTagResponse | None:
    if not isinstance(payload, dict):
        raise ValueError("Gemini analysis competency tag must be an object.")
    try:
        tag = AnalyzedCompetencyTagResponse(
            competencyTagId=payload["competencyTagId"],
            score=payload["score"],
        )
    except (KeyError, TypeError, ValidationError) as exception:
        raise ValueError("Gemini analysis competency tag is malformed.") from exception

    if tag.competencyTagId not in candidate_ids:
        return None
    if tag.competencyTagId in seen_ids:
        return None
    if tag.score < 0.0 or tag.score > 1.0:
        return None

    seen_ids.add(tag.competencyTagId)
    return tag


def extract_embedding_values(result) -> list[float]:
    embeddings = getattr(result, "embeddings", None)
    if embeddings:
        first_embedding = embeddings[0]
        values = getattr(first_embedding, "values", None)
        if values is not None:
            return list(values)

    embedding = getattr(result, "embedding", None)
    if embedding is not None:
        values = getattr(embedding, "values", embedding)
        return list(values)

    raise ValueError("Gemini embedding response is empty.")
