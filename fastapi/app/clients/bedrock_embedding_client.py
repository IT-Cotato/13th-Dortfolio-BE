import json
from time import perf_counter
from uuid import UUID

from app.clients.bedrock_runtime import PROVIDER, create_bedrock_runtime_client
from app.core.config import Settings
from app.schemas.ai_observability import AiUsage


INPUT_TOKEN_COUNT_HEADER = "x-amzn-bedrock-input-token-count"


class BedrockEmbeddingClient:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.client = create_bedrock_runtime_client(settings)

    def embed_text(
        self,
        text: str,
        input_type: str,
        request_id: UUID,
    ) -> tuple[list[float], AiUsage]:
        started_at = perf_counter()
        try:
            response = self.client.invoke_model(
                modelId=self.settings.bedrock_embedding_model_id,
                contentType="application/json",
                accept="application/json",
                body=json.dumps(
                    {
                        "texts": [text],
                        "input_type": input_type,
                        "embedding_types": ["float"],
                        "output_dimension": self.settings.embedding_dimension,
                    }
                ),
            )
            payload = json.loads(response["body"].read())
        finally:
            latency_ms = int((perf_counter() - started_at) * 1000)

        embedding = extract_embedding_values(payload, self.settings.embedding_dimension)
        usage = AiUsage(
            requestId=request_id,
            provider=PROVIDER,
            modelId=self.settings.bedrock_embedding_model_id,
            inputTokens=input_token_count(response),
            outputTokens=None,
            latencyMs=latency_ms,
        )
        return embedding, usage


def input_token_count(response: dict) -> int | None:
    headers = response.get("ResponseMetadata", {}).get("HTTPHeaders") or {}
    value = headers.get(INPUT_TOKEN_COUNT_HEADER)
    try:
        return int(value) if value is not None else None
    except (TypeError, ValueError):
        return None


def extract_embedding_values(payload: dict, expected_dimension: int) -> list[float]:
    embeddings = payload.get("embeddings") if isinstance(payload, dict) else None
    # embedding_types를 지정하면 {"float": [[...]]}, 지정하지 않으면 [[...]] 형태로 온다.
    if isinstance(embeddings, dict):
        embeddings = embeddings.get("float")
    if not embeddings or not isinstance(embeddings[0], list):
        raise ValueError("Bedrock embedding response is empty.")

    values = embeddings[0]
    if len(values) != expected_dimension:
        raise ValueError(
            "Bedrock embedding dimension mismatch. "
            f"expected={expected_dimension}, actual={len(values)}"
        )
    return [float(value) for value in values]
