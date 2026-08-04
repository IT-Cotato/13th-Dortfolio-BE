import unittest
from types import SimpleNamespace
from unittest.mock import patch

from app.schemas.matching import QuestionEmbeddingRequest
from app.services.matching_service import embed_question


class MatchingServiceTest(unittest.TestCase):
    @patch("app.services.matching_service.get_settings")
    def test_embed_question_uses_local_embedding_when_gemini_key_is_absent(self, get_settings):
        get_settings.return_value = SimpleNamespace(
            gemini_api_key=None,
            allow_local_analysis=True,
        )
        response = embed_question(QuestionEmbeddingRequest(question="목표 달성 경험"))

        self.assertEqual(response.embeddingModel, "dortfolio-local-hash-v1")
        self.assertEqual(len(response.embedding), 3072)


if __name__ == "__main__":
    unittest.main()
