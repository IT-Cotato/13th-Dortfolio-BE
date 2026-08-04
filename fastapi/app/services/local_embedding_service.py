from hashlib import sha256

LOCAL_EMBEDDING_MODEL = "dortfolio-local-hash-v1"
LOCAL_EMBEDDING_DIMENSIONS = 3072


def create_local_embedding(source_text: str) -> list[float]:
    values: list[float] = []
    seed = source_text.encode("utf-8")
    round_index = 0

    while len(values) < LOCAL_EMBEDDING_DIMENSIONS:
        digest = sha256(seed + round_index.to_bytes(4, "big")).digest()
        values.extend(((byte / 255.0) * 2.0) - 1.0 for byte in digest)
        round_index += 1

    return values[:LOCAL_EMBEDDING_DIMENSIONS]
