CREATE TEMPORARY TABLE record_answer_question_history
(
    history_question_id uuid PRIMARY KEY,
    template_id uuid NOT NULL,
    source_question_id uuid NOT NULL,
    question_description varchar(100),
    question_text varchar(30) NOT NULL,
    required boolean NOT NULL,
    sort_order integer NOT NULL
) ON COMMIT DROP;

INSERT INTO record_answer_question_history (
    history_question_id,
    template_id,
    source_question_id,
    question_description,
    question_text,
    required,
    sort_order
)
SELECT gen_random_uuid(),
       snapshot.template_id,
       snapshot.source_question_id,
       snapshot.question_description,
       snapshot.question_text,
       snapshot.required,
       snapshot.sort_order
FROM (
    SELECT DISTINCT record.template_id,
                    answer.template_question_id AS source_question_id,
                    answer.question_description,
                    answer.question_text,
                    answer.required,
                    answer.sort_order
    FROM record_answers answer
    JOIN records record ON record.id = answer.record_id
    LEFT JOIN template_questions question ON question.id = answer.template_question_id
    WHERE question.id IS NULL
       OR question.template_id <> record.template_id
       OR question.question_text IS DISTINCT FROM answer.question_text
       OR question.description IS DISTINCT FROM answer.question_description
       OR question.required IS DISTINCT FROM answer.required
       OR question.sort_order IS DISTINCT FROM answer.sort_order
) snapshot;

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
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       NULL,
       CURRENT_TIMESTAMP,
       history.question_description,
       history.question_text,
       history.required,
       history.sort_order,
       history.template_id
FROM record_answer_question_history history;

UPDATE record_answers answer
SET template_question_id = history.history_question_id
FROM records record, record_answer_question_history history
WHERE record.id = answer.record_id
  AND history.template_id = record.template_id
  AND history.source_question_id = answer.template_question_id
  AND history.question_text = answer.question_text
  AND history.question_description IS NOT DISTINCT FROM answer.question_description
  AND history.required = answer.required
  AND history.sort_order = answer.sort_order;

ALTER TABLE template_questions
    DROP CONSTRAINT fk_template_questions_template;

ALTER TABLE template_questions
    ADD CONSTRAINT fk_template_questions_template
        FOREIGN KEY (template_id) REFERENCES templates ON DELETE CASCADE;

ALTER TABLE record_answers
    ADD CONSTRAINT fk_record_answers_template_question
        FOREIGN KEY (template_question_id) REFERENCES template_questions;

ALTER TABLE record_answers
    DROP COLUMN question_description,
    DROP COLUMN question_text,
    DROP COLUMN required,
    DROP COLUMN sort_order;
