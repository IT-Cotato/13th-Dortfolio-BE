from functools import lru_cache
from os import getenv


# DB의 벡터 컬럼·인덱스 차원(Flyway V24)과 맞물려 있으므로 환경변수로 바꾸지 않는다.
EMBEDDING_DIMENSION = 1536


def positive_integer(name: str, default: str) -> int:
    value = int(getenv(name, default))
    if value <= 0:
        raise ValueError(f"{name} must be positive.")
    return value


class Settings:
    record_analysis_max_output_tokens: int = positive_integer("RECORD_ANALYSIS_MAX_OUTPUT_TOKENS", "8192")
    insight_recommendation_max_output_tokens: int = positive_integer("INSIGHT_RECOMMENDATION_MAX_OUTPUT_TOKENS", "8192")
    # S3는 AWS_REGION을 쓴다. Bedrock만 다른 리전을 써야 하면 BEDROCK_REGION으로 덮어쓴다.
    bedrock_region: str = getenv("BEDROCK_REGION") or getenv("AWS_REGION") or "ap-northeast-2"
    # Bedrock 호출에 쓰는 모델 ID 또는 추론 프로파일 ID.
    bedrock_generation_model_id: str = (
        getenv("BEDROCK_GENERATION_MODEL_ID") or "global.anthropic.claude-haiku-4-5-20251001-v1:0"
    )
    bedrock_embedding_model_id: str = getenv("BEDROCK_EMBEDDING_MODEL_ID") or "global.cohere.embed-v4:0"
    # DB의 embedding_model 컬럼과 부분 인덱스에 저장되는 논리 이름.
    # Bedrock 호출 ID가 바뀌어도 기존 임베딩과 인덱스를 계속 쓸 수 있도록 호출 ID와 분리한다.
    embedding_model_name: str = getenv("EMBEDDING_MODEL_NAME") or "cohere-embed-v4"
    embedding_dimension: int = EMBEDDING_DIMENSION
    bedrock_http_timeout_seconds: float = float(getenv("BEDROCK_HTTP_TIMEOUT_SECONDS") or "25")
    bedrock_connect_timeout_seconds: float = float(getenv("BEDROCK_CONNECT_TIMEOUT_SECONDS") or "5")


@lru_cache
def get_settings() -> Settings:
    return Settings()
