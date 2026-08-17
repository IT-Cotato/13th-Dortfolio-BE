import unittest
from types import SimpleNamespace
from unittest.mock import patch
from uuid import UUID, uuid4

from fastapi import HTTPException, status

from app.clients.gemini_record_analysis_client import (
    build_analysis_prompt,
    parse_analysis_payload,
)
from app.schemas.record_analysis import RecordAnalysisRequest
from app.services.record_analysis_service import analyze_record


class RecordAnalysisServiceTest(unittest.TestCase):
    @patch("app.services.record_analysis_service.get_settings")
    def test_analyze_record_rejects_request_when_gemini_key_is_absent(self, get_settings):
        get_settings.return_value = SimpleNamespace(gemini_api_key=None)

        with self.assertRaises(HTTPException) as context:
            analyze_record(None)

        self.assertEqual(context.exception.status_code, status.HTTP_503_SERVICE_UNAVAILABLE)

    def test_prompt_includes_strength_judgement_context_and_maximum_count(self):
        request, candidate_ids = analysis_request()

        prompt = build_analysis_prompt(request)

        self.assertIn("description: 후보 전용 구조적 문제 해결 설명", prompt)
        self.assertIn("evaluationCriteria: 원인 규명 후 직접 개선까지 실행했는지", prompt)
        self.assertIn("positiveExample: 캐시 병목을 찾아 응답 시간을 40% 줄임", prompt)
        self.assertIn("negativeExample: 문제를 확인만 하고 담당자에게 전달함", prompt)
        self.assertIn("최대 2개", prompt)
        self.assertIn(str(candidate_ids[0]), prompt)

    def test_parse_analysis_payload_accepts_up_to_two_candidate_ids(self):
        request, candidate_ids = analysis_request()

        summary, evidence, selected_ids = parse_analysis_payload(
            {
                "summary": "문제를 분석하고 해결했습니다.",
                "evidenceSnippets": ["병목을 찾아 개선했습니다."],
                "strengthTagIds": [str(candidate_id) for candidate_id in candidate_ids],
            },
            request,
        )

        self.assertEqual(summary, "문제를 분석하고 해결했습니다.")
        self.assertEqual(evidence, ["병목을 찾아 개선했습니다."])
        self.assertEqual(selected_ids, candidate_ids)

    def test_parse_analysis_payload_rejects_id_outside_candidates(self):
        request, _ = analysis_request()

        with self.assertRaisesRegex(ValueError, "outside the candidates"):
            parse_analysis_payload(
                {
                    "summary": "요약",
                    "evidenceSnippets": ["근거"],
                    "strengthTagIds": [str(uuid4())],
                },
                request,
            )

    def test_parse_analysis_payload_rejects_more_than_maximum_count(self):
        request, candidate_ids = analysis_request(candidate_count=3)

        with self.assertRaisesRegex(ValueError, "too many"):
            parse_analysis_payload(
                {
                    "summary": "요약",
                    "evidenceSnippets": ["근거"],
                    "strengthTagIds": [str(candidate_id) for candidate_id in candidate_ids],
                },
                request,
            )


def analysis_request(candidate_count: int = 2) -> tuple[RecordAnalysisRequest, list[UUID]]:
    candidate_ids = [uuid4() for _ in range(candidate_count)]
    return RecordAnalysisRequest.model_validate(
        {
            "recordId": str(uuid4()),
            "title": "추천 알고리즘 개선",
            "activity": {"title": "프로젝트", "description": "백엔드 개선"},
            "template": {"title": "문제 해결 경험"},
            "answers": [{"questionText": "무엇을 했나요?", "answerText": "병목을 찾아 개선했습니다."}],
            "memos": [],
            "strengthTagCandidates": [
                {
                    "id": str(candidate_id),
                    "name": f"강점 {index}",
                    "description": "후보 전용 구조적 문제 해결 설명",
                    "evaluationCriteria": "원인 규명 후 직접 개선까지 실행했는지",
                    "positiveExample": "캐시 병목을 찾아 응답 시간을 40% 줄임",
                    "negativeExample": "문제를 확인만 하고 담당자에게 전달함",
                    "cosineSimilarity": 0.8 - index * 0.1,
                }
                for index, candidate_id in enumerate(candidate_ids)
            ],
            "maxStrengthCount": 2,
        }
    ), candidate_ids


if __name__ == "__main__":
    unittest.main()
