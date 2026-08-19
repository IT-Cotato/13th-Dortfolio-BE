import json

from google import genai
from google.genai import types
from pydantic import ValidationError

from app.core.config import Settings
from app.schemas.insight import (
    InsightRecommendationRequest,
    InsightRecommendationResponse,
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
                response_schema=InsightRecommendationResponse,
                http_options=types.HttpOptions(
                    timeout=int(
                        self.settings
                        .gemini_http_timeout_seconds
                        * 1000
                    )
                ),
            ),
        )

        return parse_recommendation_response(
            response.text,
            request,
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

    return f"""
다음 5개 직무 역량을 충분히 보여주는 기록이 있는지 각각 판단하세요.

규칙:
- 모든 직무 역량에 대해 결과를 정확히 하나씩 반환합니다.
- 각 직무 역량을 충분히 보여주는 기록이 있을 때만 해당 역량의 후보 중 하나를 선택합니다.
- 적합한 기록이 없거나 후보가 비어 있으면 matched=false로 응답합니다.
- jobCompetencyId는 각 직무 역량의 요청 값을 그대로 반환합니다.
- matched=true이면 recordId는 해당 직무 역량의 후보 목록에 포함된 값만 반환합니다.
- matched=true이면 reason은 해당 기록이 역량을 보여주는 이유를 한국어 한 문장으로 작성합니다.
- matched=false이면 recordId와 reason은 null로 반환합니다.
- 요청에 없는 역량이나 후보에 없는 기록을 새로 만들지 않습니다.
- 동일한 직무 역량을 중복해서 반환하지 않습니다.
- JSON 객체만 반환합니다.

응답 형식:
{{
  "recommendations": [
    {{
      "matched": true,
      "jobCompetencyId": "요청에 포함된 직무 역량 UUID",
      "recordId": "해당 역량의 후보 목록에 포함된 UUID",
      "reason": "추천 이유 한 문장"
    }}
  ]
}}

직무:
- id: {request.jobId}
- 이름: {request.jobName}

직무 역량과 역량별 후보 기록:
{competency_json}
""".strip()


def parse_recommendation_response(
    response_text: str | None,
    request: InsightRecommendationRequest,
) -> InsightRecommendationResponse:
    if not response_text:
        raise ValueError(
            "Gemini recommendation response is empty."
        )

    try:
        payload = json.loads(response_text)
        response = InsightRecommendationResponse.model_validate(
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

    return InsightRecommendationResponse(
        recommendations=[
            response_by_id[competency.jobCompetencyId]
            for competency in request.competencies
        ]
    )
