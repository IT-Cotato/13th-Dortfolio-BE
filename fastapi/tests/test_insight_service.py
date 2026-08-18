import unittest
import json
from types import SimpleNamespace
from unittest.mock import patch
from uuid import uuid4

from fastapi import HTTPException

from app.schemas.insight import (
    InsightRecommendationCandidate,
    InsightRecommendationRequest,
)
from app.services.insight_service import (
    generate_insight_recommendation,
)
from app.clients.gemini_insight_client import (
    parse_recommendation_response,
)


class InsightServiceTest(unittest.TestCase):
    def request(self) -> InsightRecommendationRequest:
        return InsightRecommendationRequest(
            jobId=uuid4(),
            jobName="백엔드 개발자",
            jobCompetencyId=uuid4(),
            competencyName="문제 해결",
            competencyDescription="문제 해결 역량",
            candidates=[
                InsightRecommendationCandidate(
                    recordId=uuid4(),
                    recordTitle="성능 개선",
                    templateName="문제 해결",
                    summary="성능을 개선했습니다.",
                    evidenceSnippets=["쿼리를 최적화했습니다."],
                    similarity=0.91,
                )
            ],
        )

    @patch("app.services.insight_service.get_settings")
    def test_rejects_when_api_key_is_missing(
        self,
        get_settings,
    ):
        get_settings.return_value = SimpleNamespace(
            gemini_api_key=None
        )

        with self.assertRaises(HTTPException) as context:
            generate_insight_recommendation(self.request())

        self.assertEqual(
            context.exception.status_code,
            503,
        )

    def test_parses_valid_candidate_response(self):
        request = self.request()
        candidate_id = request.candidates[0].recordId

        response = parse_recommendation_response(
            json.dumps({
                "matched": True,
                "jobCompetencyId": str(request.jobCompetencyId),
                "recordId": str(candidate_id),
                "reason": "문제 해결 과정이 구체적으로 드러납니다.",
            }),
            request,
        )

        self.assertEqual(response.recordId, candidate_id)

    def test_rejects_record_outside_candidates(self):
        request = self.request()

        with self.assertRaises(ValueError):
            parse_recommendation_response(
                json.dumps({
                    "matched": True,
                    "jobCompetencyId": str(request.jobCompetencyId),
                    "recordId": str(uuid4()),
                    "reason": "추천 이유",
                }),
                request,
            )

    def test_rejects_another_competency(self):
        request = self.request()

        with self.assertRaises(ValueError):
            parse_recommendation_response(
                json.dumps({
                    "matched": True,
                    "jobCompetencyId": str(uuid4()),
                    "recordId": str(request.candidates[0].recordId),
                    "reason": "추천 이유",
                }),
                request,
            )

    def test_rejects_blank_reason(self):
        request = self.request()

        with self.assertRaises(ValueError):
            parse_recommendation_response(
                json.dumps({
                    "matched": True,
                    "jobCompetencyId": str(request.jobCompetencyId),
                    "recordId": str(request.candidates[0].recordId),
                    "reason": " ",
                }),
                request,
            )

    def test_parses_no_match_response(self):
        request = self.request()

        response = parse_recommendation_response(
            json.dumps({
                "matched": False,
                "jobCompetencyId": str(request.jobCompetencyId),
                "recordId": None,
                "reason": None,
            }),
            request,
        )

        self.assertFalse(response.matched)
        self.assertIsNone(response.recordId)

    def test_rejects_no_match_response_containing_reason(self):
        request = self.request()

        with self.assertRaises(ValueError):
            parse_recommendation_response(
                json.dumps({
                    "matched": False,
                    "jobCompetencyId": str(request.jobCompetencyId),
                    "recordId": None,
                    "reason": "추천 이유",
                }),
                request,
            )

    def test_rejects_no_match_response_containing_record(self):
        request = self.request()

        with self.assertRaises(ValueError):
            parse_recommendation_response(
                json.dumps({
                    "matched": False,
                    "jobCompetencyId": str(request.jobCompetencyId),
                    "recordId": str(request.candidates[0].recordId),
                    "reason": None,
                }),
                request,
            )

    def test_rejects_invalid_json(self):
        with self.assertRaises(ValueError):
            parse_recommendation_response(
                "not-json",
                self.request(),
            )


if __name__ == "__main__":
    unittest.main()
