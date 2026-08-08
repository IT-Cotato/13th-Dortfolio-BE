ALTER TABLE record_answers
    ADD CONSTRAINT fk_record_answers_template_question
        FOREIGN KEY (template_question_id) REFERENCES template_questions;

ALTER TABLE record_answers
    DROP COLUMN question_description,
    DROP COLUMN question_text,
    DROP COLUMN required,
    DROP COLUMN sort_order;

ALTER TABLE template_questions
    DROP COLUMN deleted_at;
