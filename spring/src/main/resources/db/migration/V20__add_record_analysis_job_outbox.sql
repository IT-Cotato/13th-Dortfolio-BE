CREATE TABLE record_analysis_jobs (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    record_id uuid NOT NULL,
    status varchar(255) NOT NULL CHECK (status IN ('READY', 'RUNNING', 'COMPLETED')),
    attempt_count integer NOT NULL,
    started_at timestamp(6),
    claim_token uuid,
    lease_expires_at timestamp(6),
    analysis_generation bigint NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_record_analysis_jobs_record
        FOREIGN KEY (record_id) REFERENCES records ON DELETE CASCADE
);

CREATE INDEX idx_record_analysis_jobs_ready
    ON record_analysis_jobs (status, created_at);

CREATE UNIQUE INDEX uk_record_analysis_jobs_record
    ON record_analysis_jobs (record_id);

ALTER TABLE record_analysis
    ADD COLUMN analysis_generation bigint NOT NULL DEFAULT 0;
