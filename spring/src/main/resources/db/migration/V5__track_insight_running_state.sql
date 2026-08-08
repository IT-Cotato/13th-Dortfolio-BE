ALTER TABLE insights
    ADD COLUMN started_at timestamp(6);

ALTER TABLE insights
    DROP CONSTRAINT IF EXISTS insights_status_check;

ALTER TABLE insights
    ADD CONSTRAINT insights_status_check
        CHECK (status IN (
            'PENDING',
            'RUNNING',
            'COMPLETED',
            'FAILED'
        ));

DROP INDEX IF EXISTS uq_insights_user_pending;

CREATE UNIQUE INDEX uq_insights_user_active
    ON insights (user_id)
    WHERE status IN ('PENDING', 'RUNNING');
