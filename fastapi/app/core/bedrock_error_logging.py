import logging

from app.core.ai_error import bedrock_error_code, bedrock_http_status


logger = logging.getLogger(__name__)


def log_bedrock_api_error(
    *,
    operation: str,
    model: str,
    exception: Exception,
) -> None:
    # 예외 메시지에는 프롬프트나 응답 일부가 섞일 수 있어 상태와 오류 코드만 남긴다.
    logger.warning(
        "Bedrock API request failed. operation=%s, model=%s, status=%s, code=%s",
        operation,
        model,
        bedrock_http_status(exception),
        bedrock_error_code(exception),
    )
