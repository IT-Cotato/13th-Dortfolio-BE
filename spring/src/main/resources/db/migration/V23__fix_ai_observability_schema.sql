ALTER TABLE ai_requests
    RENAME COLUMN update_at TO updated_at;

ALTER TABLE ai_requests
    DROP CONSTRAINT ck_ai_requests_feature;

ALTER TABLE ai_requests
    ADD CONSTRAINT ck_ai_requests_feature
    CHECK (
        feature IN (
            'RECORD_EMBEDDING',
            'RECORD_ANALYSIS',
            'QUESTION_EMBEDDING',
            'INSIGHT_RECOMMENDATION',
            'STRENGTH_TAG_EMBEDDING',
            'JOB_COMPETENCY_EMBEDDING'
        )
    );
