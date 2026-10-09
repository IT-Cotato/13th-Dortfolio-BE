import logging
from types import SimpleNamespace

from app.core.gemini_error_logging import log_gemini_api_error
from app.schemas.ai_observability import billable_output_tokens


def test_billable_output_includes_thinking_tokens():
    metadata = SimpleNamespace(candidates_token_count=30, thoughts_token_count=70)

    assert billable_output_tokens(metadata) == 100
    assert billable_output_tokens(None) is None


def test_provider_error_log_excludes_exception_message_and_traceback(caplog):
    class PrivateProviderError(Exception):
        code = 429

    with caplog.at_level(logging.WARNING):
        log_gemini_api_error(
            operation="analysis",
            model="test-model",
            exception=PrivateProviderError("private prompt and response"),
        )

    assert len(caplog.records) == 1
    assert "status=429" in caplog.text
    assert "private prompt and response" not in caplog.text
    assert caplog.records[0].exc_info is None
