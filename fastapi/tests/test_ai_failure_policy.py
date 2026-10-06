from types import SimpleNamespace
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient
from httpx import ConnectError, ReadTimeout

from app.core.ai_error import provider_error
from app.core.config import Settings
from app.clients.gemini_record_analysis_client import GeminiRecordAnalysisClient
from app.clients.gemini_insight_client import GeminiInsightClient
from test_record_analysis_service import analysis_request
import test_insight_service as insight_fixtures
from main import app


@pytest.mark.parametrize("code,expected_status", [(429, "RATE_LIMITED"), (504, "TIMEOUT"), (401, "CONFIGURATION_ERROR"), (403, "CONFIGURATION_ERROR"), (500, "PROVIDER_ERROR")])
def test_provider_status_and_retry_after_are_preserved(code, expected_status):
    error = provider_error(SimpleNamespace(code=code, response=SimpleNamespace(headers={"retry-after": "120"})))
    assert error.status_code == code
    assert error.status == expected_status
    assert error.headers["Retry-After"] == "120"
    assert error.error_code == f"HTTP_{code}"


@pytest.mark.parametrize("path,payload,settings_path", [
    ("/ai/records/analyze", analysis_request()[0].model_dump(mode="json"), "app.services.record_analysis_service.get_settings"),
    ("/ai/insights/recommendation", insight_fixtures.InsightServiceTest().request().model_dump(mode="json"), "app.services.insight_service.get_settings"),
    ("/ai/matching/question-embedding", {"question": "질문"}, "app.services.matching_service.get_settings"),
])
def test_configuration_error_uses_typed_body_for_all_ai_routes(path, payload, settings_path):
    with patch(settings_path, return_value=SimpleNamespace(gemini_api_key=None)):
        response = TestClient(app).post(path, json=payload)
    assert response.status_code == 503
    assert response.json()["status"] == "CONFIGURATION_ERROR"
    assert response.json()["errorCode"] == "API_KEY_MISSING"


@pytest.mark.parametrize("failure,expected_status,expected_code", [
    (ValueError("PRIVATE ANSWER"), 422, "SCHEMA_VALIDATION_FAILED"),
    (ReadTimeout("PRIVATE ANSWER"), 504, "TIMEOUT"),
    (ConnectError("PRIVATE ANSWER"), 502, "CONNECTION_ERROR"),
])
def test_record_failures_have_safe_error_codes(failure, expected_status, expected_code):
    request, _ = analysis_request()
    with patch("app.services.record_analysis_service.get_settings", return_value=SimpleNamespace(gemini_api_key="test-key")), patch("app.services.record_analysis_service.GeminiRecordAnalysisClient") as client:
        client.return_value.analyze_record.side_effect = failure
        response = TestClient(app).post("/ai/records/analyze", json=request.model_dump(mode="json"))
    assert response.status_code == expected_status
    assert response.json()["errorCode"] == expected_code
    assert "PRIVATE ANSWER" not in response.text


@pytest.mark.parametrize("client_class,module", [
    (GeminiRecordAnalysisClient, "app.clients.gemini_record_analysis_client"),
    (GeminiInsightClient, "app.clients.gemini_insight_client"),
])
def test_sdk_makes_one_attempt_per_spring_call(client_class, module):
    with patch(f"{module}.genai.Client") as client:
        client_class(Settings())
    assert client.call_args.kwargs["http_options"].retry_options.attempts == 1
