CREATE TABLE record_analysis_jobs (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    record_id uuid NOT NULL,
    status varchar(255) NOT NULL CHECK (status IN ('READY', 'RUNNING', 'COMPLETED')),
    attempt_count integer NOT NULL,
    started_at timestamp(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_record_analysis_jobs_record
        FOREIGN KEY (record_id) REFERENCES records ON DELETE CASCADE
);

CREATE INDEX idx_record_analysis_jobs_ready
    ON record_analysis_jobs (status, created_at);

UPDATE record_analysis
SET ai_analysis_status = 'FAILED',
    summary = NULL,
    evidence_snippets = NULL,
    analyzed_at = NULL,
    analyzed_record_updated_at = NULL,
    failure_reason = COALESCE(last_failure_reason, failure_reason),
    failure_retryable = last_failure_retryable
WHERE ai_analysis_status = 'COMPLETED'
  AND last_attempt_failed = true;
