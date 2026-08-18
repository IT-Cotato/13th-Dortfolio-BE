import json
from uuid import UUID

from google import genai
from google.genai import types
from app.core.config import Settings
from app.schemas.record_analysis import RecordAnalysisRequest


class GeminiRecordAnalysisClient:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.client = genai.Client(
            api_key=settings.gemini_api_key,
            http_options=types.HttpOptions(timeout=int(settings.gemini_http_timeout_seconds * 1000)),
        )

    def analyze_record(self, request: RecordAnalysisRequest) -> tuple[str, list[str], list[UUID]]:
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
    strength_tags = "\n".join(
        (
            f"- id: {candidate.id}\n"
            f"  name: {candidate.name}\n"
            f"  description: {candidate.description}\n"
            f"  evaluationCriteria: {candidate.evaluationCriteria}\n"
            f"  positiveExample: {candidate.positiveExample}\n"
            f"  negativeExample: {candidate.negativeExample}\n"
            f"  cosineSimilarity: {candidate.cosineSimilarity}"
        )
        for candidate in request.strengthTagCandidates
    )

    return f"""
아래 기록을 분석해서 JSON만 반환해 주세요.

규칙:
- summary는 한국어 1문장으로 작성합니다.
- evidenceSnippets는 답변/메모 원문에서 핵심 근거 문장만 1~5개 추출합니다.
- strengthTagIds는 반드시 후보군 id 중에서만 선택합니다.
- 정의, 판단 기준, 적합 예시와 부적합 예시를 함께 고려합니다.
- 코사인 유사도는 후보 검색 결과이며 최종 판단의 유일한 근거로 사용하지 않습니다.
- 최대 {request.maxStrengthCount}개를 선택합니다.
- 후보군이 비어 있거나 기록에서 확인할 수 있는 강점이 없으면 빈 배열을 반환합니다.
- 새로운 태그 id를 만들지 않습니다.

응답 JSON 형식:
{{
  "summary": "string",
  "evidenceSnippets": ["string"],
  "strengthTagIds": ["uuid"]
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
{strength_tags}
""".strip()


def parse_analysis_payload(
    payload: dict,
    request: RecordAnalysisRequest,
) -> tuple[str, list[str], list[UUID]]:
    if not isinstance(payload, dict):
        raise ValueError("Gemini analysis response must be a JSON object.")
    required_fields = {"summary", "evidenceSnippets", "strengthTagIds"}
    if not required_fields.issubset(payload):
        raise ValueError("Gemini analysis response is missing required fields.")

    summary = payload["summary"]
    evidence_snippets = payload["evidenceSnippets"]
    raw_strength_tag_ids = payload["strengthTagIds"]
    if not isinstance(summary, str):
        raise ValueError("Gemini analysis summary must be a string.")
    if not isinstance(evidence_snippets, list) or not all(
        isinstance(item, str) and item.strip()
        for item in evidence_snippets
    ):
        raise ValueError("Gemini analysis evidenceSnippets must contain non-blank strings.")
    if not 1 <= len(evidence_snippets) <= 5:
        raise ValueError("Gemini analysis evidenceSnippets must contain between 1 and 5 items.")
    if not isinstance(raw_strength_tag_ids, list):
        raise ValueError("Gemini analysis strengthTagIds must be an array.")

    candidate_ids = {candidate.id for candidate in request.strengthTagCandidates}
    try:
        strength_tag_ids = [UUID(strength_tag_id) for strength_tag_id in raw_strength_tag_ids]
    except (TypeError, ValueError) as exception:
        raise ValueError("Gemini analysis strengthTagIds are malformed.") from exception
    if len(strength_tag_ids) != len(set(strength_tag_ids)):
        raise ValueError("Gemini analysis strengthTagIds must be unique.")
    if len(strength_tag_ids) > request.maxStrengthCount:
        raise ValueError("Gemini analysis selected too many strength tags.")
    if not set(strength_tag_ids).issubset(candidate_ids):
        raise ValueError("Gemini analysis selected a strength tag outside the candidates.")

    return summary, evidence_snippets, strength_tag_ids


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
