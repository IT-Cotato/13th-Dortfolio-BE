from time import perf_counter
from uuid import UUID

from app.clients.bedrock_runtime import (
    PROVIDER,
    create_bedrock_runtime_client,
    extract_tool_input,
    tool_config,
)
from app.core.config import Settings
from app.core.generation import check_generation_response, inference_config
from app.prompts.registry import RECORD_ANALYSIS_PROMPT
from app.schemas.ai_observability import AiUsage
from app.schemas.record_analysis import RecordAnalysisRequest, RecordAnalysisOutput, RecordAnalysisResponse


RECORD_ANALYSIS_TOOL_NAME = "submit_record_analysis"
RECORD_ANALYSIS_TOOL_DESCRIPTION = "기록 분석 결과(요약, 근거 문장, 강점 태그)를 제출합니다."


class BedrockRecordAnalysisClient:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.client = create_bedrock_runtime_client(settings)

    def analyze_record(
        self,
        request: RecordAnalysisRequest,
        request_id: UUID,
    ) -> RecordAnalysisResponse:
        prompt = build_analysis_prompt(request)

        started_at = perf_counter()
        try:
            response = self.client.converse(
                modelId=self.settings.bedrock_generation_model_id,
                messages=[{"role": "user", "content": [{"text": prompt}]}],
                inferenceConfig=inference_config(self.settings, "record_analysis"),
                toolConfig=tool_config(
                    RECORD_ANALYSIS_TOOL_NAME,
                    RECORD_ANALYSIS_TOOL_DESCRIPTION,
                    RecordAnalysisOutput.model_json_schema(),
                ),
            )
        finally:
            latency_ms = int((perf_counter() - started_at) * 1000)

        check_generation_response(response, "record_analysis")
        payload = extract_tool_input(response, RECORD_ANALYSIS_TOOL_NAME)
        summary, evidence_snippets, strength_tag_ids = parse_analysis_payload(
            payload,
            request,
        )

        token_usage = response.get("usage") or {}
        usage = AiUsage(
            requestId=request_id,
            provider=PROVIDER,
            modelId=self.settings.bedrock_generation_model_id,
            inputTokens=token_usage.get("inputTokens"),
            outputTokens=token_usage.get("outputTokens"),
            latencyMs=latency_ms,
        )

        return RecordAnalysisResponse(
            summary=summary, evidenceSnippets=evidence_snippets,
            strengthTagIds=strength_tag_ids, usage=usage,
            metadata=RECORD_ANALYSIS_PROMPT.metadata(self.settings),
        )


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
        raise ValueError("Bedrock analysis response must be a JSON object.")
    required_fields = {"summary", "evidenceSnippets", "strengthTagIds"}
    if not required_fields.issubset(payload):
        raise ValueError("Bedrock analysis response is missing required fields.")

    summary = payload["summary"]
    evidence_snippets = payload["evidenceSnippets"]
    raw_strength_tag_ids = payload["strengthTagIds"]
    if not isinstance(summary, str):
        raise ValueError("Bedrock analysis summary must be a string.")
    if not isinstance(evidence_snippets, list) or not all(
        isinstance(item, str) and item.strip()
        for item in evidence_snippets
    ):
        raise ValueError("Bedrock analysis evidenceSnippets must contain non-blank strings.")
    if not 1 <= len(evidence_snippets) <= 5:
        raise ValueError("Bedrock analysis evidenceSnippets must contain between 1 and 5 items.")
    if not isinstance(raw_strength_tag_ids, list):
        raise ValueError("Bedrock analysis strengthTagIds must be an array.")

    candidate_ids = {candidate.id for candidate in request.strengthTagCandidates}
    try:
        strength_tag_ids = [UUID(strength_tag_id) for strength_tag_id in raw_strength_tag_ids]
    except (TypeError, ValueError) as exception:
        raise ValueError("Bedrock analysis strengthTagIds are malformed.") from exception
    if len(strength_tag_ids) != len(set(strength_tag_ids)):
        raise ValueError("Bedrock analysis strengthTagIds must be unique.")
    if len(strength_tag_ids) > request.maxStrengthCount:
        raise ValueError("Bedrock analysis selected too many strength tags.")
    if not set(strength_tag_ids).issubset(candidate_ids):
        raise ValueError("Bedrock analysis selected a strength tag outside the candidates.")

    return summary, evidence_snippets, strength_tag_ids
