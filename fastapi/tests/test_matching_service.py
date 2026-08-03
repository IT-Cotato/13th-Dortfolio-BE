import os
import unittest

os.environ["FASTAPI_ALLOW_LOCAL_ANALYSIS"] = "true"

from app.core.config import get_settings
from app.schemas.matching import QuestionEmbeddingRequest
from app.services.matching_service import embed_question


class MatchingServiceTest(unittest.TestCase):
    def setUp(self):
        get_settings.cache_clear()

    def test_embed_question_uses_local_embedding_when_gemini_key_is_absent(self):
        response = embed_question(QuestionEmbeddingRequest(question="목표 달성 경험"))

        self.assertEqual(response.embeddingModel, "dortfolio-local-hash-v1")
        self.assertEqual(len(response.embedding), 3072)


if __name__ == "__main__":
    unittest.main()
