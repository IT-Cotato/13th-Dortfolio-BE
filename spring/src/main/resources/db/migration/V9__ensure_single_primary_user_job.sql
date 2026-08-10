-- 동시에 희망 직무를 변경해도 사용자별 대표 직무는 하나만 유지한다.
WITH ranked_primary_jobs AS (
    SELECT id,
           row_number() OVER (PARTITION BY user_id ORDER BY updated_at DESC, id DESC) AS row_number
    FROM user_jobs
    WHERE is_primary = true
)
UPDATE user_jobs
SET is_primary = false
WHERE id IN (
    SELECT id
    FROM ranked_primary_jobs
    WHERE row_number > 1
);

CREATE UNIQUE INDEX CONCURRENTLY uq_user_jobs_single_primary
    ON user_jobs (user_id)
    WHERE is_primary = true;
