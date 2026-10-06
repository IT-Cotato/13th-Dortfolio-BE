import json
import logging
from uuid import UUID

from google import genai
from google.genai import types
from app.core.config import Settings
from app.core.generation import generation_config, check_generation_response
from app.prompts.registry import RECORD_ANALYSIS_PROMPT
from app.schemas.record_analysis import RecordAnalysisRequest, RecordAnalysisOutput, RecordAnalysisResponse


logger = logging.getLogger(__name__)


class GeminiRecordAnalysisClient:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.client = genai.Client(
            api_key=settings.gemini_api_key,
            http_options=types.HttpOptions(
                timeout=int(settings.gemini_http_timeout_seconds * 1000),
                retry_options=types.HttpRetryOptions(attempts=1),
            ),
        )

    def analyze_record(self, request: RecordAnalysisRequest) -> RecordAnalysisResponse:
        prompt = build_analysis_prompt(request)
        answers = "\n".join(
            f"- 질문: {answer.questionText}\n  답변: {answer.answerText}"
            for answer in request.answers
        )
        strength_tags = build_strength_tags(request)
        logger.warning(
            "Gemini record analysis prompt prepared. recordId=%s, promptChars=%s, "
            "answerCount=%s, answerChars=%s, strengthCandidateCount=%s, "
            "strengthCandidateChars=%s",
            request.recordId,
            len(prompt),
            len(request.answers),
            len(answers),
            len(request.strengthTagCandidates),
            len(strength_tags),
        )
        response = self.client.models.generate_content(
            model=self.settings.gemini_generation_model,
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=RecordAnalysisOutput,
                **generation_config(self.settings, "record_analysis"),
            ),
        )
        check_generation_response(response, "record_analysis")
        payload = json.loads(response.text or "{}")
        summary, evidence_snippets, strength_tag_ids = parse_analysis_payload(payload, request)
        return RecordAnalysisResponse(
            summary=summary,
            evidenceSnippets=evidence_snippets,
            strengthTagIds=strength_tag_ids,
            metadata=RECORD_ANALYSIS_PROMPT.metadata(self.settings),
        )

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
    strength_tags = build_strength_tags(request)

    return RECORD_ANALYSIS_PROMPT.render(
        max_strength_count=request.maxStrengthCount,
        record_id=request.recordId,
        title=request.title,
        activity_title=request.activity.title,
        activity_description=request.activity.description or "",
        template_title=request.template.title,
        answers=answers,
        strength_tags=strength_tags,
    )


def build_strength_tags(request: RecordAnalysisRequest) -> str:
    return "\n".join(
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
