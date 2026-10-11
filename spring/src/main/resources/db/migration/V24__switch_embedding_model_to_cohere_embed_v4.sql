-- 임베딩 모델 전환: gemini-embedding-2 (3072차원) -> cohere-embed-v4 (1536차원)
--
-- 서로 다른 모델이 만든 벡터는 비교할 수 없으므로 기존 임베딩은 모두 폐기하고
-- 배포 후 새 모델로 다시 생성한다. (docs/DEPLOYMENT.md의 임베딩 배치 실행 절 참고)

DROP INDEX IF EXISTS idx_record_embeddings_gemini_embedding_2_hvc;

DELETE FROM record_embeddings;
DELETE FROM job_competency_embeddings;
DELETE FROM strength_tag_embeddings;

ALTER TABLE job_competency_embeddings
    ALTER COLUMN embedding TYPE vector(1536);

ALTER TABLE strength_tag_embeddings
    ALTER COLUMN embedding TYPE vector(1536);

CREATE INDEX IF NOT EXISTS idx_record_embeddings_cohere_embed_v4_hvc
ON record_embeddings
USING hnsw ((embedding::halfvec(1536)) halfvec_cosine_ops)
WHERE embedding_model = 'cohere-embed-v4';

-- 이미 분석이 끝난 기록은 임베딩만 사라진 상태가 되므로 재분석 대상으로 표시한다.
-- 요약과 분석 상태는 그대로 두어 사용자 화면은 바뀌지 않고,
-- 강점 태그 임베딩 생성 후 실행하는 준비 전 실패(RA008) 재분석 배치가 이 기록들을 다시 분석한다.
UPDATE record_analysis
SET last_attempt_failed = true,
    last_failure_retryable = true,
    last_failure_reason = 'RA008 임베딩 모델 전환으로 재분석이 필요합니다.'
WHERE ai_analysis_status = 'COMPLETED';
