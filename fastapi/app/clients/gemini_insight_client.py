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
    candidate_json = json.dumps(
        [
            candidate.model_dump(mode="json")
            for candidate in request.candidates
        ],
        ensure_ascii=False,
        indent=2,
    )

    return f"""
다음 직무 역량을 가장 잘 보여주는 기록 하나를 선택하세요.

규칙:
- 반드시 제공된 후보 기록 중 하나만 선택합니다.
- jobCompetencyId는 요청의 값을 그대로 반환합니다.
- recordId는 후보 목록에 포함된 값만 반환합니다.
- reason은 해당 기록이 역량을 보여주는 이유를 한국어 한 문장으로 작성합니다.
- 후보에 없는 기록이나 역량을 새로 만들지 않습니다.
- JSON 객체만 반환합니다.

응답 형식:
{{
  "jobCompetencyId": "{request.jobCompetencyId}",
  "recordId": "후보 목록에 포함된 UUID",
  "reason": "추천 이유 한 문장"
}}

직무:
- id: {request.jobId}
- 이름: {request.jobName}

직무 역량:
- id: {request.jobCompetencyId}
- 이름: {request.competencyName}
- 설명: {request.competencyDescription or ""}

후보 기록:
{candidate_json}
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

    if response.jobCompetencyId != request.jobCompetencyId:
        raise ValueError(
            "Gemini returned another job competency."
        )

    candidate_ids = {
        candidate.recordId
        for candidate in request.candidates
    }

    if response.recordId not in candidate_ids:
        raise ValueError(
            "Gemini selected a record outside candidates."
        )

    return response