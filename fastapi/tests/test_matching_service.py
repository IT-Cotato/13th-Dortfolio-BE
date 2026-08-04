import unittest
from types import SimpleNamespace
from unittest.mock import patch

from fastapi import HTTPException, status

from app.schemas.matching import QuestionEmbeddingRequest
from app.services.matching_service import embed_question


class MatchingServiceTest(unittest.TestCase):
    @patch("app.services.matching_service.get_settings")
    def test_embed_question_rejects_request_when_gemini_key_is_absent(self, get_settings):
        get_settings.return_value = SimpleNamespace(
            gemini_api_key=None,
        )

        with self.assertRaises(HTTPException) as context:
            embed_question(QuestionEmbeddingRequest(question="목표 달성 경험"))

        self.assertEqual(context.exception.status_code, status.HTTP_503_SERVICE_UNAVAILABLE)


if __name__ == "__main__":
    unittest.main()
