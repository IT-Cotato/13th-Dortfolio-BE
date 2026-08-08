ALTER TABLE insight_job_recommendations
    ADD COLUMN sort_order_snapshot integer;

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY insight_id
               ORDER BY created_at, id
           ) - 1 AS sort_order
    FROM insight_job_recommendations
)
UPDATE insight_job_recommendations recommendation
SET sort_order_snapshot = ranked.sort_order
FROM ranked
WHERE recommendation.id = ranked.id;

ALTER TABLE insight_job_recommendations
    ALTER COLUMN sort_order_snapshot SET NOT NULL;
