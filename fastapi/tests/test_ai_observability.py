import logging

from botocore.exceptions import ConnectTimeoutError, EndpointConnectionError, ReadTimeoutError

from app.core.ai_error import bedrock_http_status, bedrock_retry_after
from app.core.bedrock_error_logging import log_bedrock_api_error
from bedrock_fixtures import client_error


def test_http_status_comes_from_bedrock_response_metadata():
    assert bedrock_http_status(client_error("ThrottlingException", 429)) == 429
    assert bedrock_retry_after(
        client_error("ThrottlingException", 429, {"retry-after": "30"})
    ) == "30"
    assert bedrock_retry_after(client_error("ThrottlingException", 429)) is None


def test_timeouts_without_a_response_are_reported_as_gateway_timeout():
    assert bedrock_http_status(ReadTimeoutError(endpoint_url="https://bedrock.test")) == 504
    assert bedrock_http_status(ConnectTimeoutError(endpoint_url="https://bedrock.test")) == 504
    assert bedrock_http_status(EndpointConnectionError(endpoint_url="https://bedrock.test")) is None


def test_provider_error_log_excludes_exception_message_and_traceback(caplog):
    with caplog.at_level(logging.WARNING):
        log_bedrock_api_error(
            operation="analysis",
            model="test-model",
            exception=client_error(
                "ThrottlingException",
                429,
                message="private prompt and response",
            ),
        )

    assert len(caplog.records) == 1
    assert "status=429" in caplog.text
    assert "code=ThrottlingException" in caplog.text
    assert "private prompt and response" not in caplog.text
    assert caplog.records[0].exc_info is None
