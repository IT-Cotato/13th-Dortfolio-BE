import json
from time import perf_counter
from uuid import UUID

from pydantic import ValidationError

from app.clients.bedrock_runtime import (
    PROVIDER,
    create_bedrock_runtime_client,
    extract_tool_input,
    tool_config,
)
from app.core.config import Settings
from app.core.generation import check_generation_response, inference_config
from app.prompts.registry import INSIGHT_RECOMMENDATION_PROMPT
from app.schemas.ai_observability import AiUsage
from app.schemas.insight import (
    InsightRecommendationRequest,
    InsightRecommendationResponse,
    InsightRecommendationOutput,
)


INSIGHT_RECOMMENDATION_TOOL_NAME = "submit_insight_recommendations"
INSIGHT_RECOMMENDATION_TOOL_DESCRIPTION = "직무 역량별 추천 기록 판단 결과를 제출합니다."


class BedrockInsightClient:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.client = create_bedrock_runtime_client(settings)

    def generate_recommendation(
        self,
        request: InsightRecommendationRequest,
        request_id: UUID,
    ) -> InsightRecommendationResponse:
        prompt = build_recommendation_prompt(request)

        started_at = perf_counter()
        try:
            response = self.client.converse(
                modelId=self.settings.bedrock_generation_model_id,
                messages=[{"role": "user", "content": [{"text": prompt}]}],
                inferenceConfig=inference_config(self.settings, "insight_recommendation"),
                toolConfig=tool_config(
                    INSIGHT_RECOMMENDATION_TOOL_NAME,
                    INSIGHT_RECOMMENDATION_TOOL_DESCRIPTION,
                    InsightRecommendationOutput.model_json_schema(),
                ),
            )
        finally:
            latency_ms = int((perf_counter() - started_at) * 1000)

        check_generation_response(response, "insight_recommendation")
        payload = extract_tool_input(response, INSIGHT_RECOMMENDATION_TOOL_NAME)
        result = parse_recommendation_response(
            json.dumps(payload, ensure_ascii=False),
            request,
        )
        token_usage = response.get("usage") or {}
        return InsightRecommendationResponse(
            recommendations=result.recommendations,
            metadata=INSIGHT_RECOMMENDATION_PROMPT.metadata(self.settings),
            usage=AiUsage(
                requestId=request_id,
                provider=PROVIDER,
                modelId=self.settings.bedrock_generation_model_id,
                inputTokens=token_usage.get("inputTokens"),
                outputTokens=token_usage.get("outputTokens"),
                latencyMs=latency_ms,
            ),
        )


def build_recommendation_prompt(
    request: InsightRecommendationRequest,
) -> str:
    competency_json = json.dumps(
        [
            competency.model_dump(mode="json")
            for competency in request.competencies
        ],
        ensure_ascii=False,
        indent=2,
    )

    return INSIGHT_RECOMMENDATION_PROMPT.render(
        job_id=request.jobId,
        job_name=request.jobName,
        competency_json=competency_json,
    )


def parse_recommendation_response(
    response_text: str | None,
    request: InsightRecommendationRequest,
) -> InsightRecommendationOutput:
    if not response_text:
        raise ValueError(
            "Bedrock recommendation response is empty."
        )

    try:
        payload = json.loads(response_text)
        response = InsightRecommendationOutput.model_validate(
            payload
        )
    except (
        json.JSONDecodeError,
        ValidationError,
        TypeError,
    ) as exception:
        raise ValueError(
            "Bedrock recommendation response is invalid."
        ) from exception

    request_by_id = {
        competency.jobCompetencyId: competency
        for competency in request.competencies
    }
    response_by_id = {
        recommendation.jobCompetencyId: recommendation
        for recommendation in response.recommendations
    }

    if len(response_by_id) != len(response.recommendations):
        raise ValueError(
            "Bedrock returned duplicate job competencies."
        )

    if response_by_id.keys() != request_by_id.keys():
        raise ValueError(
            "Bedrock returned missing or unknown job competencies."
        )

    for competency_id, recommendation in response_by_id.items():
        candidate_ids = {
            candidate.recordId
            for candidate in request_by_id[competency_id].candidates
        }

        if (
            recommendation.matched
            and recommendation.recordId not in candidate_ids
        ):
            raise ValueError(
                "Bedrock selected a record outside candidates."
            )

    return InsightRecommendationOutput(
        recommendations=[
            response_by_id[competency.jobCompetencyId]
            for competency in request.competencies
        ]
    )
