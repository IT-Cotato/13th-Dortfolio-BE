import json
import unittest
from types import SimpleNamespace
from unittest.mock import patch
from uuid import uuid4

from fastapi import HTTPException
from google.genai import errors
from pydantic import ValidationError

from app.clients.gemini_insight_client import (
    build_recommendation_prompt,
    parse_recommendation_response,
)
from app.schemas.insight import (
    InsightRecommendationCandidate,
    InsightRecommendationCompetency,
    InsightRecommendationRequest,
)
from app.services.insight_service import generate_insight_recommendation


class InsightServiceTest(unittest.TestCase):
    def request(self) -> InsightRecommendationRequest:
        competencies = []

        for index in range(5):
            competencies.append(
                InsightRecommendationCompetency(
                    jobCompetencyId=uuid4(),
                    competencyName=f"직무 역량 {index + 1}",
                    competencyDescription=f"직무 역량 {index + 1} 설명",
                    candidates=[
                        InsightRecommendationCandidate(
                            recordId=uuid4(),
                            recordTitle=f"경험 기록 {index + 1}",
                            templateName="문제 해결",
                            summary="문제를 분석하고 개선했습니다.",
                            evidenceSnippets=["병목을 찾아 개선했습니다."],
                            similarity=0.91 - index * 0.01,
                        )
                    ],
                )
            )

        return InsightRecommendationRequest(
            jobId=uuid4(),
            jobName="백엔드 개발자",
            competencies=competencies,
        )

    def valid_recommendations(
        self,
        request: InsightRecommendationRequest,
    ) -> list[dict]:
        return [
            {
                "matched": True,
                "jobCompetencyId": str(competency.jobCompetencyId),
                "recordId": str(competency.candidates[0].recordId),
                "reason": f"{competency.competencyName}을 보여주는 기록입니다.",
            }
            for competency in request.competencies
        ]

    @patch("app.services.insight_service.get_settings")
    def test_rejects_when_api_key_is_missing(self, get_settings):
        get_settings.return_value = SimpleNamespace(gemini_api_key=None)

        with self.assertRaises(HTTPException) as context:
            generate_insight_recommendation(self.request())

        self.assertEqual(context.exception.status_code, 503)

    def test_request_requires_exactly_five_competencies(self):
        request = self.request()

        with self.assertRaises(ValidationError):
            InsightRecommendationRequest(
                jobId=request.jobId,
                jobName=request.jobName,
                competencies=request.competencies[:4],
            )

    def test_request_rejects_duplicate_competencies(self):
        request = self.request()

        with self.assertRaises(ValidationError):
            InsightRecommendationRequest(
                jobId=request.jobId,
                jobName=request.jobName,
                competencies=[
                    *request.competencies[:4],
                    request.competencies[0],
                ],
            )

    def test_prompt_contains_all_competencies_and_candidates(self):
        request = self.request()

        prompt = build_recommendation_prompt(request)

        for competency in request.competencies:
            self.assertIn(str(competency.jobCompetencyId), prompt)
            self.assertIn(competency.competencyName, prompt)
            self.assertIn(str(competency.candidates[0].recordId), prompt)

    def test_parses_and_orders_all_recommendations(self):
        request = self.request()
        recommendations = self.valid_recommendations(request)

        response = parse_recommendation_response(
            json.dumps({"recommendations": list(reversed(recommendations))}),
            request,
        )

        self.assertEqual(
            [item.jobCompetencyId for item in response.recommendations],
            [item.jobCompetencyId for item in request.competencies],
        )

    def test_parses_no_match_response(self):
        request = self.request()
        recommendations = self.valid_recommendations(request)
        recommendations[0] = {
            "matched": False,
            "jobCompetencyId": str(
                request.competencies[0].jobCompetencyId
            ),
            "recordId": None,
            "reason": None,
        }

        response = parse_recommendation_response(
            json.dumps({"recommendations": recommendations}),
            request,
        )

        self.assertFalse(response.recommendations[0].matched)
        self.assertIsNone(response.recommendations[0].recordId)

    def test_rejects_record_outside_competency_candidates(self):
        request = self.request()
        recommendations = self.valid_recommendations(request)
        recommendations[0]["recordId"] = str(uuid4())

        with self.assertRaisesRegex(ValueError, "outside candidates"):
            parse_recommendation_response(
                json.dumps({"recommendations": recommendations}),
                request,
            )

    def test_rejects_duplicate_response_competencies(self):
        request = self.request()
        recommendations = self.valid_recommendations(request)
        recommendations[-1]["jobCompetencyId"] = recommendations[0][
            "jobCompetencyId"
        ]

        with self.assertRaisesRegex(ValueError, "duplicate"):
            parse_recommendation_response(
                json.dumps({"recommendations": recommendations}),
                request,
            )

    def test_rejects_unknown_response_competency(self):
        request = self.request()
        recommendations = self.valid_recommendations(request)
        recommendations[-1]["jobCompetencyId"] = str(uuid4())

        with self.assertRaisesRegex(ValueError, "missing or unknown"):
            parse_recommendation_response(
                json.dumps({"recommendations": recommendations}),
                request,
            )

    def test_rejects_blank_reason(self):
        request = self.request()
        recommendations = self.valid_recommendations(request)
        recommendations[0]["reason"] = " "

        with self.assertRaisesRegex(ValueError, "invalid"):
            parse_recommendation_response(
                json.dumps({"recommendations": recommendations}),
                request,
            )

    def test_rejects_invalid_json(self):
        with self.assertRaisesRegex(ValueError, "invalid"):
            parse_recommendation_response("not-json", self.request())

    @patch("app.services.insight_service.GeminiInsightClient")
    @patch("app.services.insight_service.get_settings")
    def test_preserves_gemini_rate_limit_status(
        self,
        get_settings,
        client_type,
    ):
        get_settings.return_value = SimpleNamespace(
            gemini_api_key="test-key",
            gemini_generation_model="gemini-3.6-flash",
        )
        client_type.return_value.generate_recommendation.side_effect = (
            errors.APIError(
                429,
                {"error": {"message": "Resource exhausted"}},
                SimpleNamespace(headers={"retry-after": "30"}),
            )
        )

        with self.assertRaises(HTTPException) as context:
            generate_insight_recommendation(self.request())

        self.assertEqual(context.exception.status_code, 429)
        self.assertEqual(context.exception.headers, {"Retry-After": "30"})

    @patch("app.services.insight_service.GeminiInsightClient")
    @patch("app.services.insight_service.get_settings")
    def test_preserves_non_retryable_gemini_status(
        self,
        get_settings,
        client_type,
    ):
        get_settings.return_value = SimpleNamespace(
            gemini_api_key="test-key",
            gemini_generation_model="gemini-3.6-flash",
        )
        client_type.return_value.generate_recommendation.side_effect = (
            errors.APIError(
                403,
                {"error": {"message": "Permission denied"}},
            )
        )

        with self.assertRaises(HTTPException) as context:
            generate_insight_recommendation(self.request())

        self.assertEqual(context.exception.status_code, 403)


if __name__ == "__main__":
    unittest.main()
