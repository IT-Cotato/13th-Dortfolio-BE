import json
import unittest
from uuid import uuid4
from types import SimpleNamespace
from unittest.mock import patch

from fastapi import HTTPException, status

from app.clients.bedrock_embedding_client import (
    BedrockEmbeddingClient,
    extract_embedding_values,
)
from app.core.ai_error import AiError
from app.schemas.matching import QuestionEmbeddingRequest
from app.services.matching_service import embed_question
from bedrock_fixtures import client_error, embedding_response


def settings(dimension: int = 4):
    return SimpleNamespace(
        bedrock_embedding_model_id="test-embedding-model",
        embedding_model_name="cohere-embed-v4",
        embedding_dimension=dimension,
    )


class MatchingServiceTest(unittest.TestCase):
    @patch("app.services.matching_service.bedrock_credentials_available", return_value=False)
    def test_embed_question_rejects_request_when_aws_credentials_are_absent(self, _credentials):
        with self.assertRaises(HTTPException) as context:
            embed_question(QuestionEmbeddingRequest(question="목표 달성 경험"), uuid4())

        self.assertEqual(context.exception.status_code, status.HTTP_503_SERVICE_UNAVAILABLE)

    @patch("app.services.matching_service.bedrock_credentials_available", return_value=True)
    @patch("app.clients.bedrock_embedding_client.create_bedrock_runtime_client")
    @patch("app.services.matching_service.get_settings")
    def test_embed_question_returns_logical_model_name_and_bedrock_usage(
        self, get_settings, create_client, _credentials
    ):
        get_settings.return_value = settings()
        create_client.return_value.invoke_model.return_value = embedding_response(
            [0.1, 0.2, 0.3, 0.4], input_tokens=12
        )

        request_id = uuid4()
        response = embed_question(
            QuestionEmbeddingRequest(question="목표 달성 경험", inputType="search_query"),
            request_id,
        )

        # DB에 저장되는 이름은 Bedrock 호출 ID와 분리된 논리 이름이다.
        self.assertEqual(response.embeddingModel, "cohere-embed-v4")
        self.assertEqual(response.embedding, [0.1, 0.2, 0.3, 0.4])
        self.assertEqual(response.usage.requestId, request_id)
        self.assertEqual(response.usage.provider, "BEDROCK")
        self.assertEqual(response.usage.modelId, "test-embedding-model")
        self.assertEqual(response.usage.inputTokens, 12)
        self.assertIsNone(response.usage.outputTokens)
        call = create_client.return_value.invoke_model.call_args.kwargs
        self.assertEqual(call["modelId"], "test-embedding-model")
        self.assertEqual(
            json.loads(call["body"]),
            {
                "texts": ["목표 달성 경험"],
                "input_type": "search_query",
                "embedding_types": ["float"],
                "output_dimension": 4,
            },
        )

    @patch("app.services.matching_service.bedrock_credentials_available", return_value=True)
    @patch("app.clients.bedrock_embedding_client.create_bedrock_runtime_client")
    @patch("app.services.matching_service.get_settings")
    def test_embed_question_preserves_bedrock_throttling(
        self, get_settings, create_client, _credentials
    ):
        get_settings.return_value = settings()
        create_client.return_value.invoke_model.side_effect = client_error(
            "ThrottlingException", 429, {"retry-after": "5"}, operation="InvokeModel"
        )

        with self.assertRaises(AiError) as context:
            embed_question(QuestionEmbeddingRequest(question="목표 달성 경험"), uuid4())

        self.assertEqual(context.exception.http_status, 429)
        self.assertEqual(context.exception.status, "RATE_LIMITED")
        self.assertEqual(context.exception.retry_after, "5")
        self.assertEqual(context.exception.model_id, "test-embedding-model")

    @patch("app.services.matching_service.bedrock_credentials_available", return_value=True)
    @patch("app.clients.bedrock_embedding_client.create_bedrock_runtime_client")
    @patch("app.services.matching_service.get_settings")
    def test_embed_question_rejects_unexpected_dimension(
        self, get_settings, create_client, _credentials
    ):
        get_settings.return_value = settings(dimension=8)
        create_client.return_value.invoke_model.return_value = embedding_response([0.1, 0.2])

        with self.assertRaises(AiError) as context:
            embed_question(QuestionEmbeddingRequest(question="목표 달성 경험"), uuid4())

        self.assertEqual(context.exception.status, "INVALID_RESPONSE")

    def test_request_defaults_to_document_input_type(self):
        self.assertEqual(
            QuestionEmbeddingRequest(question="기록 본문").inputType,
            "search_document",
        )

    @patch("app.clients.bedrock_embedding_client.create_bedrock_runtime_client")
    def test_usage_tokens_are_optional_when_header_is_missing(self, create_client):
        create_client.return_value.invoke_model.return_value = embedding_response(
            [0.1, 0.2, 0.3, 0.4], input_tokens=None
        )

        _, usage = BedrockEmbeddingClient(settings()).embed_text("본문", "search_document", uuid4())

        self.assertIsNone(usage.inputTokens)

    def test_extract_embedding_accepts_plain_list_response(self):
        self.assertEqual(extract_embedding_values({"embeddings": [[1, 2]]}, 2), [1.0, 2.0])

    def test_extract_embedding_rejects_empty_response(self):
        with self.assertRaisesRegex(ValueError, "empty"):
            extract_embedding_values({"embeddings": {"float": []}}, 3)


if __name__ == "__main__":
    unittest.main()
