CREATE TEMPORARY TABLE record_answer_question_history
(
    record_answer_id uuid PRIMARY KEY,
    history_question_id uuid NOT NULL
) ON COMMIT DROP;

INSERT INTO record_answer_question_history (record_answer_id, history_question_id)
SELECT answer.id, gen_random_uuid()
FROM record_answers answer
JOIN records record ON record.id = answer.record_id
LEFT JOIN template_questions question ON question.id = answer.template_question_id
WHERE question.id IS NULL
   OR question.template_id <> record.template_id
   OR question.question_text IS DISTINCT FROM answer.question_text
   OR question.description IS DISTINCT FROM answer.question_description
   OR question.required IS DISTINCT FROM answer.required
   OR question.sort_order IS DISTINCT FROM answer.sort_order;

INSERT INTO template_questions (
    id,
    created_at,
    updated_at,
    builtin_code,
    deleted_at,
    description,
    question_text,
    required,
    sort_order,
    template_id
)
SELECT history.history_question_id,
       answer.created_at,
       answer.updated_at,
       NULL,
       CURRENT_TIMESTAMP,
       answer.question_description,
       answer.question_text,
       answer.required,
       answer.sort_order,
       record.template_id
FROM record_answer_question_history history
JOIN record_answers answer ON answer.id = history.record_answer_id
JOIN records record ON record.id = answer.record_id;

UPDATE record_answers answer
SET template_question_id = history.history_question_id
FROM record_answer_question_history history
WHERE answer.id = history.record_answer_id;

ALTER TABLE record_answers
    ADD CONSTRAINT fk_record_answers_template_question
        FOREIGN KEY (template_question_id) REFERENCES template_questions;

ALTER TABLE record_answers
    DROP COLUMN question_description,
    DROP COLUMN question_text,
    DROP COLUMN required,
    DROP COLUMN sort_order;
