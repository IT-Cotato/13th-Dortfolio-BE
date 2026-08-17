ALTER TABLE insight_job_recommendations
    ADD COLUMN match_status varchar(20);

UPDATE insight_job_recommendations
SET match_status = 'MATCHED';

ALTER TABLE insight_job_recommendations
    ALTER COLUMN record_id_snapshot DROP NOT NULL,
    ALTER COLUMN record_title_snapshot DROP NOT NULL,
    ALTER COLUMN template_name_snapshot DROP NOT NULL,
    ALTER COLUMN reason DROP NOT NULL,
    ALTER COLUMN similarity DROP NOT NULL;

ALTER TABLE insight_job_recommendations
    ADD CONSTRAINT ck_insight_job_recommendation_match_status
        CHECK (match_status IN ('MATCHED', 'NO_MATCH')) NOT VALID,
    ADD CONSTRAINT ck_insight_job_recommendation_match_status_not_null
        CHECK (match_status IS NOT NULL) NOT VALID,
    ADD CONSTRAINT ck_insight_job_recommendation_match_result
        CHECK (
            (
                match_status = 'MATCHED'
                AND record_id_snapshot IS NOT NULL
                AND record_title_snapshot IS NOT NULL
                AND template_name_snapshot IS NOT NULL
                AND reason IS NOT NULL
                AND similarity IS NOT NULL
            )
            OR
            (
                match_status = 'NO_MATCH'
                AND record_id_snapshot IS NULL
                AND record_title_snapshot IS NULL
                AND template_name_snapshot IS NULL
                AND reason IS NULL
                AND similarity IS NULL
            )
        ) NOT VALID;
