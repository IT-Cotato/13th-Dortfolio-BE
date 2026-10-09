import json
import unittest
from uuid import uuid4
from types import SimpleNamespace
from unittest.mock import patch

from google.genai import types

from app.clients.gemini_record_analysis_client import GeminiRecordAnalysisClient
from app.clients.gemini_insight_client import GeminiInsightClient
from app.core.config import Settings, positive_integer, thinking_level
from app.schemas.record_analysis import RecordAnalysisOutput
from app.schemas.insight import InsightRecommendationOutput
from app.services.record_analysis_service import analyze_record
from app.services.insight_service import generate_insight_recommendation
from test_record_analysis_service import analysis_request
import test_insight_service as insight_fixtures


class GenerationMetadataTest(unittest.TestCase):
    def settings(self):
        settings = Settings()
        settings.gemini_api_key = "test-key"
        settings.record_analysis_max_output_tokens = 4096
        settings.insight_recommendation_max_output_tokens = 8192
        settings.record_analysis_thinking_level = "low"
        settings.insight_recommendation_thinking_level = None
        return settings

    def response(self, payload, finish_reason=types.FinishReason.STOP):
        return SimpleNamespace(
            text=json.dumps(payload),
            candidates=[SimpleNamespace(finish_reason=finish_reason)],
            usage_metadata=SimpleNamespace(
                prompt_token_count=100,
                candidates_token_count=50,
                thoughts_token_count=20,
            ),
        )

    @patch("app.clients.gemini_record_analysis_client.genai.Client")
    @patch("app.services.record_analysis_service.get_settings")
    def test_record_response_attaches_server_metadata_and_uses_feature_config(self, get_settings, client_type):
        settings = self.settings()
        get_settings.return_value = settings
        request, ids = analysis_request()
        client_type.return_value.models.generate_content.return_value = self.response({
            "summary": "요약", "evidenceSnippets": ["근거"],
            "strengthTagIds": [str(ids[0])],
            "metadata": {"promptVersion": "forged"},
        })

        request_id = uuid4()
        result = analyze_record(request, request_id)
        self.assertEqual(result.usage.requestId, request_id)
        self.assertEqual(result.usage.inputTokens, 100)
        self.assertEqual(result.usage.outputTokens, 70)
        config = client_type.return_value.models.generate_content.call_args.kwargs["config"]
        self.assertEqual(config.max_output_tokens, 4096)
        self.assertEqual(config.thinking_config.thinking_level, types.ThinkingLevel.LOW)
        self.assertIs(config.response_schema, RecordAnalysisOutput)
        self.assertNotIn("metadata", RecordAnalysisOutput.model_json_schema()["properties"])
        self.assertEqual(result.metadata.promptVersion, "record_analysis.v1")
        self.assertEqual(result.metadata.schemaVersion, "record_analysis.v1")
        self.assertEqual(result.metadata.model, settings.gemini_generation_model)
        self.assertEqual(result.metadata.maxOutputTokens, 4096)
        self.assertEqual(result.metadata.thinkingLevel, "low")

    @patch("app.clients.gemini_insight_client.genai.Client")
    def test_insight_uses_its_own_limit_and_preserves_default_thinking(self, client_type):
        request = insight_fixtures.InsightServiceTest().request()
        client_type.return_value.models.generate_content.return_value = self.response({
            "recommendations": insight_fixtures.InsightServiceTest().valid_recommendations(request),
        })

        request_id = uuid4()
        result = GeminiInsightClient(self.settings()).generate_recommendation(request, request_id)
        self.assertEqual(result.usage.requestId, request_id)
        self.assertEqual(result.usage.inputTokens, 100)
        self.assertEqual(result.usage.outputTokens, 70)
        config = client_type.return_value.models.generate_content.call_args.kwargs["config"]
        self.assertEqual(config.max_output_tokens, 8192)
        self.assertIsNone(config.thinking_config)
        self.assertIs(config.response_schema, InsightRecommendationOutput)
        self.assertNotIn("metadata", InsightRecommendationOutput.model_json_schema()["properties"])
        self.assertEqual(result.metadata.promptVersion, "insight_recommendation.v1")
        self.assertEqual(result.metadata.maxOutputTokens, 8192)
        self.assertIsNone(result.metadata.thinkingLevel)

    @patch("app.clients.gemini_record_analysis_client.genai.Client")
    @patch("app.services.record_analysis_service.get_settings")
    def test_record_token_limit_is_rejected_even_when_json_is_valid(self, get_settings, client_type):
        get_settings.return_value = self.settings()
        request, _ = analysis_request()
        client_type.return_value.models.generate_content.return_value = self.response({
            "summary": "요약", "evidenceSnippets": ["근거"], "strengthTagIds": [],
        }, types.FinishReason.MAX_TOKENS)
        from fastapi import HTTPException
        with self.assertRaises(HTTPException) as context:
            analyze_record(request, uuid4())
        self.assertEqual(context.exception.status_code, 422)
        self.assertIn("token limit", context.exception.detail)

    @patch("app.clients.gemini_insight_client.genai.Client")
    @patch("app.services.insight_service.get_settings")
    def test_insight_token_limit_does_not_trigger_transient_failure_retry(self, get_settings, client_type):
        get_settings.return_value = self.settings()
        request = insight_fixtures.InsightServiceTest().request()
        client_type.return_value.models.generate_content.return_value = self.response({}, types.FinishReason.MAX_TOKENS)
        from fastapi import HTTPException
        with self.assertRaises(HTTPException) as context:
            generate_insight_recommendation(request, uuid4())
        self.assertEqual(context.exception.status_code, 422)

    def test_invalid_generation_settings_are_rejected(self):
        for value in ("0", "-1", "invalid"):
            with self.subTest(value=value), patch.dict("os.environ", {"TEST_LIMIT": value}):
                with self.assertRaises(ValueError):
                    positive_integer("TEST_LIMIT", "8192")
        with patch.dict("os.environ", {"TEST_LEVEL": "unsupported"}):
            with self.assertRaises(ValueError):
                thinking_level("TEST_LEVEL")
