import json

from google import genai
from google.genai import types
from pydantic import ValidationError

from app.core.config import Settings
from app.core.generation import generation_config, check_generation_response
from app.prompts.registry import INSIGHT_RECOMMENDATION_PROMPT
from app.schemas.insight import (
    InsightRecommendationRequest,
    InsightRecommendationResponse,
    InsightRecommendationOutput,
)


class GeminiInsightClient:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.client = genai.Client(
            api_key=settings.gemini_api_key,
            http_options=types.HttpOptions(
                timeout=int(
                    settings.gemini_http_timeout_seconds * 1000
                )
            ),
        )

    def generate_recommendation(
        self,
        request: InsightRecommendationRequest,
    ) -> InsightRecommendationResponse:
        prompt = build_recommendation_prompt(request)

        response = self.client.models.generate_content(
            model=self.settings.gemini_generation_model,
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=InsightRecommendationOutput,
                **generation_config(self.settings, "insight_recommendation"),
            ),
        )

        check_generation_response(response, "insight_recommendation")
        result = parse_recommendation_response(response.text, request)
        return InsightRecommendationResponse(
            recommendations=result.recommendations,
            metadata=INSIGHT_RECOMMENDATION_PROMPT.metadata(self.settings),
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
            "Gemini recommendation response is empty."
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
            "Gemini recommendation response is invalid."
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
            "Gemini returned duplicate job competencies."
        )

    if response_by_id.keys() != request_by_id.keys():
        raise ValueError(
            "Gemini returned missing or unknown job competencies."
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
                "Gemini selected a record outside candidates."
            )

    return InsightRecommendationOutput(
        recommendations=[
            response_by_id[competency.jobCompetencyId]
            for competency in request.competencies
        ]
    )
