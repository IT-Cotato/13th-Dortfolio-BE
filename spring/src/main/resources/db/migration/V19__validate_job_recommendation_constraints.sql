ALTER TABLE insight_job_recommendations
    VALIDATE CONSTRAINT ck_insight_job_recommendation_match_status,
    VALIDATE CONSTRAINT ck_insight_job_recommendation_match_status_not_null,
    VALIDATE CONSTRAINT ck_insight_job_recommendation_match_result;

ALTER TABLE insight_job_recommendations
    ALTER COLUMN match_status SET NOT NULL;

ALTER TABLE insight_job_recommendations
    DROP CONSTRAINT ck_insight_job_recommendation_match_status_not_null;
