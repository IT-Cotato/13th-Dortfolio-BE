import boto3
from botocore.config import Config

from app.core.ai_error import PROVIDER  # noqa: F401  (클라이언트들이 여기서 가져다 쓴다)
from app.core.config import Settings


def bedrock_credentials_available() -> bool:
    """AWS 자격 증명(환경변수 또는 인스턴스 역할)이 잡히는지 확인한다."""
    return boto3.Session().get_credentials() is not None


def create_bedrock_runtime_client(settings: Settings):
    return boto3.client(
        "bedrock-runtime",
        region_name=settings.bedrock_region,
        config=Config(
            connect_timeout=settings.bedrock_connect_timeout_seconds,
            read_timeout=settings.bedrock_http_timeout_seconds,
            # 재시도는 Spring의 재시도 정책이 맡는다. 여기서 또 재시도하면
            # Spring의 read timeout을 넘겨 요청이 겹칠 수 있다.
            retries={"total_max_attempts": 1, "mode": "standard"},
        ),
    )


def tool_config(name: str, description: str, input_schema: dict) -> dict:
    """도구 호출을 강제해 모델이 스키마에 맞는 JSON 객체만 반환하게 한다."""
    return {
        "tools": [
            {
                "toolSpec": {
                    "name": name,
                    "description": description,
                    "inputSchema": {"json": input_schema},
                }
            }
        ],
        "toolChoice": {"tool": {"name": name}},
    }


def extract_tool_input(response: dict, tool_name: str) -> dict:
    content = response.get("output", {}).get("message", {}).get("content", [])
    for block in content:
        tool_use = block.get("toolUse")
        if tool_use and tool_use.get("name") == tool_name:
            payload = tool_use.get("input")
            if isinstance(payload, dict):
                return payload

    raise ValueError("Bedrock response did not include a structured result.")
