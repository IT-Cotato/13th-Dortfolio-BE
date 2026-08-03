-- flyway:executeInTransaction=false

ALTER EXTENSION vector UPDATE;

DO $$
DECLARE
    vector_version text;
    vector_major integer;
    vector_minor integer;
BEGIN
    SELECT extversion
    INTO vector_version
    FROM pg_extension
    WHERE extname = 'vector';

    IF vector_version IS NULL THEN
        RAISE EXCEPTION 'pgvector extension is required for AI record matching.';
    END IF;

    vector_major := split_part(vector_version, '.', 1)::integer;
    vector_minor := split_part(vector_version, '.', 2)::integer;

    IF vector_major = 0 AND vector_minor < 8 THEN
        RAISE EXCEPTION 'pgvector 0.8.0 or later is required for hnsw.iterative_scan. currentVersion=%', vector_version;
    END IF;
END $$;

CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS uk_record_embedding_record_model
ON record_embeddings (record_id, embedding_model);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_record_embeddings_gemini_embedding_2_hvc
ON record_embeddings
USING hnsw ((embedding::halfvec(3072)) halfvec_cosine_ops)
WHERE embedding_model = 'gemini-embedding-2';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_record_embeddings_dortfolio_local_hash_v1_hvc
ON record_embeddings
USING hnsw ((embedding::halfvec(3072)) halfvec_cosine_ops)
WHERE embedding_model = 'dortfolio-local-hash-v1';
