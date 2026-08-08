ALTER TABLE insights
    ADD COLUMN started_at timestamp(6);

DROP INDEX uq_insights_user_pending;

CREATE UNIQUE INDEX uq_insights_user_active
    ON insights (user_id)
    WHERE status IN ('PENDING', 'RUNNING');
