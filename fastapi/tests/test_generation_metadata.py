import unittest
from uuid import uuid4
from unittest.mock import patch

from fastapi import HTTPException

from app.clients.bedrock_record_analysis_client import (
    RECORD_ANALYSIS_TOOL_NAME,
    BedrockRecordAnalysisClient,
)
from app.clients.bedrock_insight_client import (
    INSIGHT_RECOMMENDATION_TOOL_NAME,
    BedrockInsightClient,
)
from app.core.ai_error import AiError
from app.core.config import Settings, positive_integer
from app.schemas.record_analysis import RecordAnalysisOutput
from app.schemas.insight import InsightRecommendationOutput
from app.services.record_analysis_service import analyze_record
from app.services.insight_service import generate_insight_recommendation
from bedrock_fixtures import converse_response
from test_record_analysis_service import analysis_request
import test_insight_service as insight_fixtures


class GenerationMetadataTest(unittest.TestCase):
    def settings(self):
        settings = Settings()
        settings.bedrock_generation_model_id = "test-generation-model"
        settings.record_analysis_max_output_tokens = 4096
        settings.insight_recommendation_max_output_tokens = 8192
        return settings

    @patch("app.services.record_analysis_service.bedrock_credentials_available", return_value=True)
    @patch("app.clients.bedrock_record_analysis_client.create_bedrock_runtime_client")
    @patch("app.services.record_analysis_service.get_settings")
    def test_record_response_attaches_server_metadata_and_uses_feature_config(
        self, get_settings, create_client, _credentials
    ):
        settings = self.settings()
        get_settings.return_value = settings
        request, ids = analysis_request()
        create_client.return_value.converse.return_value = converse_response(
            RECORD_ANALYSIS_TOOL_NAME,
            {
                "summary": "요약", "evidenceSnippets": ["근거"],
                "strengthTagIds": [str(ids[0])],
                "metadata": {"promptVersion": "forged"},
            },
        )

        request_id = uuid4()
        result = analyze_record(request, request_id)
        self.assertEqual(result.usage.requestId, request_id)
        self.assertEqual(result.usage.provider, "BEDROCK")
        self.assertEqual(result.usage.modelId, "test-generation-model")
        self.assertEqual(result.usage.inputTokens, 100)
        self.assertEqual(result.usage.outputTokens, 70)
        call = create_client.return_value.converse.call_args.kwargs
        self.assertEqual(call["modelId"], "test-generation-model")
        self.assertEqual(call["inferenceConfig"], {"maxTokens": 4096})
        tool = call["toolConfig"]["tools"][0]["toolSpec"]
        self.assertEqual(tool["inputSchema"]["json"], RecordAnalysisOutput.model_json_schema())
        self.assertEqual(
            call["toolConfig"]["toolChoice"],
            {"tool": {"name": RECORD_ANALYSIS_TOOL_NAME}},
        )
        self.assertNotIn("metadata", RecordAnalysisOutput.model_json_schema()["properties"])
        self.assertEqual(result.metadata.promptVersion, "record_analysis.v1")
        self.assertEqual(result.metadata.schemaVersion, "record_analysis.v1")
        self.assertEqual(result.metadata.model, "test-generation-model")
        self.assertEqual(result.metadata.maxOutputTokens, 4096)
        self.assertIsNone(result.metadata.thinkingLevel)

    @patch("app.clients.bedrock_insight_client.create_bedrock_runtime_client")
    def test_insight_uses_its_own_limit(self, create_client):
        request = insight_fixtures.InsightServiceTest().request()
        create_client.return_value.converse.return_value = converse_response(
            INSIGHT_RECOMMENDATION_TOOL_NAME,
            {"recommendations": insight_fixtures.InsightServiceTest().valid_recommendations(request)},
        )

        request_id = uuid4()
        result = BedrockInsightClient(self.settings()).generate_recommendation(request, request_id)
        self.assertEqual(result.usage.requestId, request_id)
        self.assertEqual(result.usage.inputTokens, 100)
        self.assertEqual(result.usage.outputTokens, 70)
        call = create_client.return_value.converse.call_args.kwargs
        self.assertEqual(call["inferenceConfig"], {"maxTokens": 8192})
        tool = call["toolConfig"]["tools"][0]["toolSpec"]
        self.assertEqual(tool["inputSchema"]["json"], InsightRecommendationOutput.model_json_schema())
        self.assertNotIn("metadata", InsightRecommendationOutput.model_json_schema()["properties"])
        self.assertEqual(result.metadata.promptVersion, "insight_recommendation.v1")
        self.assertEqual(result.metadata.maxOutputTokens, 8192)
        self.assertIsNone(result.metadata.thinkingLevel)

    @patch("app.services.record_analysis_service.bedrock_credentials_available", return_value=True)
    @patch("app.clients.bedrock_record_analysis_client.create_bedrock_runtime_client")
    @patch("app.services.record_analysis_service.get_settings")
    def test_record_token_limit_is_rejected_even_when_json_is_valid(
        self, get_settings, create_client, _credentials
    ):
        get_settings.return_value = self.settings()
        request, _ = analysis_request()
        create_client.return_value.converse.return_value = converse_response(
            RECORD_ANALYSIS_TOOL_NAME,
            {"summary": "요약", "evidenceSnippets": ["근거"], "strengthTagIds": []},
            stop_reason="max_tokens",
        )
        with self.assertRaises(HTTPException) as context:
            analyze_record(request, uuid4())
        self.assertEqual(context.exception.status_code, 422)
        self.assertIn("token limit", context.exception.detail)

    @patch("app.services.insight_service.bedrock_credentials_available", return_value=True)
    @patch("app.clients.bedrock_insight_client.create_bedrock_runtime_client")
    @patch("app.services.insight_service.get_settings")
    def test_insight_token_limit_does_not_trigger_transient_failure_retry(
        self, get_settings, create_client, _credentials
    ):
        get_settings.return_value = self.settings()
        request = insight_fixtures.InsightServiceTest().request()
        create_client.return_value.converse.return_value = converse_response(
            INSIGHT_RECOMMENDATION_TOOL_NAME, {}, stop_reason="max_tokens",
        )
        with self.assertRaises(HTTPException) as context:
            generate_insight_recommendation(request, uuid4())
        self.assertEqual(context.exception.status_code, 422)

    @patch("app.clients.bedrock_record_analysis_client.create_bedrock_runtime_client")
    def test_response_without_tool_use_is_an_invalid_response(self, create_client):
        request, _ = analysis_request()
        create_client.return_value.converse.return_value = {
            "stopReason": "end_turn",
            "output": {"message": {"role": "assistant", "content": [{"text": "분석 결과입니다."}]}},
            "usage": {"inputTokens": 10, "outputTokens": 5},
        }
        with self.assertRaisesRegex(ValueError, "structured result"):
            BedrockRecordAnalysisClient(self.settings()).analyze_record(request, uuid4())

    @patch("app.services.record_analysis_service.bedrock_credentials_available", return_value=True)
    @patch("app.clients.bedrock_record_analysis_client.create_bedrock_runtime_client")
    @patch("app.services.record_analysis_service.get_settings")
    def test_schema_violation_is_reported_as_invalid_response(
        self, get_settings, create_client, _credentials
    ):
        get_settings.return_value = self.settings()
        request, _ = analysis_request()
        create_client.return_value.converse.return_value = converse_response(
            RECORD_ANALYSIS_TOOL_NAME,
            {"summary": "요약", "evidenceSnippets": [], "strengthTagIds": []},
        )
        with self.assertRaises(AiError) as context:
            analyze_record(request, uuid4())
        self.assertEqual(context.exception.status, "INVALID_RESPONSE")
        self.assertEqual(context.exception.provider, "BEDROCK")

    def test_invalid_generation_settings_are_rejected(self):
        for value in ("0", "-1", "invalid"):
            with self.subTest(value=value), patch.dict("os.environ", {"TEST_LIMIT": value}):
                with self.assertRaises(ValueError):
                    positive_integer("TEST_LIMIT", "8192")
