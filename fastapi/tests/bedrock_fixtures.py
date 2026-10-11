import io
import json

from botocore.exceptions import ClientError


def client_error(
    code: str,
    status_code: int,
    headers: dict[str, str] | None = None,
    operation: str = "Converse",
    message: str = "private provider message",
) -> ClientError:
    return ClientError(
        {
            "Error": {"Code": code, "Message": message},
            "ResponseMetadata": {
                "HTTPStatusCode": status_code,
                "HTTPHeaders": headers or {},
            },
        },
        operation,
    )


def converse_response(
    tool_name: str,
    payload: dict,
    stop_reason: str = "tool_use",
    input_tokens: int = 100,
    output_tokens: int = 70,
) -> dict:
    return {
        "stopReason": stop_reason,
        "output": {
            "message": {
                "role": "assistant",
                "content": [{"toolUse": {"toolUseId": "tool-1", "name": tool_name, "input": payload}}],
            }
        },
        "usage": {
            "inputTokens": input_tokens,
            "outputTokens": output_tokens,
            "totalTokens": input_tokens + output_tokens,
        },
    }


def embedding_response(values: list[float], input_tokens: int | None = 12) -> dict:
    headers = {}
    if input_tokens is not None:
        headers["x-amzn-bedrock-input-token-count"] = str(input_tokens)
    return {
        "body": io.BytesIO(json.dumps({"embeddings": {"float": [values]}}).encode()),
        "ResponseMetadata": {"HTTPStatusCode": 200, "HTTPHeaders": headers},
    }
