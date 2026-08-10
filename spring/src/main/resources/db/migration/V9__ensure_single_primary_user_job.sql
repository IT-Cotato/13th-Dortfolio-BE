-- 기존 쓰기 트랜잭션을 모두 기다린 상태에서 임시 보호 장치를 설치한다.
BEGIN;

LOCK TABLE user_jobs IN SHARE ROW EXCLUSIVE MODE;

CREATE FUNCTION guard_single_primary_user_job() RETURNS trigger AS $$
BEGIN
    IF NEW.is_primary = true THEN
        PERFORM pg_advisory_xact_lock(hashtextextended(NEW.user_id::text, 0));

        IF EXISTS (
            SELECT 1
            FROM user_jobs
            WHERE user_id = NEW.user_id
              AND is_primary = true
              AND id IS DISTINCT FROM NEW.id
        ) THEN
            RAISE EXCEPTION 'user % already has a primary job', NEW.user_id
                USING ERRCODE = 'unique_violation';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_guard_single_primary_user_job
    BEFORE INSERT OR UPDATE OF user_id, is_primary ON user_jobs
    FOR EACH ROW
    EXECUTE FUNCTION guard_single_primary_user_job();

COMMIT;

-- 기존 중복 데이터는 가장 최근에 변경된 대표 직무 하나만 유지한다.
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

DROP TRIGGER trg_guard_single_primary_user_job ON user_jobs;
DROP FUNCTION guard_single_primary_user_job();
