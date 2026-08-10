-- 회원 탈퇴는 사용자와 사용자가 소유한 데이터를 모두 Hard Delete한다.
-- 기존 외래키의 기본 RESTRICT 동작 때문에 데이터가 있는 사용자의 탈퇴가 실패하므로
-- 사용자 소유 Aggregate의 외래키를 ON DELETE CASCADE로 통일한다.

ALTER TABLE user_jobs
    DROP CONSTRAINT fk_user_jobs_user,
    ADD CONSTRAINT fk_user_jobs_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE user_term_agreements
    DROP CONSTRAINT fk_user_term_agreements_user,
    ADD CONSTRAINT fk_user_term_agreements_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE password_reset_tokens
    DROP CONSTRAINT fk_password_reset_tokens_user,
    ADD CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE activity_type
    DROP CONSTRAINT fk_activity_type_user,
    ADD CONSTRAINT fk_activity_type_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE activity
    DROP CONSTRAINT fk_activity_user,
    ADD CONSTRAINT fk_activity_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE templates
    DROP CONSTRAINT fk_templates_user,
    ADD CONSTRAINT fk_templates_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE memos
    DROP CONSTRAINT fk_memos_user,
    ADD CONSTRAINT fk_memos_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE records
    DROP CONSTRAINT fk_records_user,
    ADD CONSTRAINT fk_records_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE ai_insights
    DROP CONSTRAINT fk_ai_insights_user,
    ADD CONSTRAINT fk_ai_insights_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE insights
    DROP CONSTRAINT fk_insights_user,
    ADD CONSTRAINT fk_insights_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE template_questions
    DROP CONSTRAINT fk_template_questions_template,
    ADD CONSTRAINT fk_template_questions_template
        FOREIGN KEY (template_id) REFERENCES templates (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE memo_images
    DROP CONSTRAINT fk_memo_images_memo,
    ADD CONSTRAINT fk_memo_images_memo
        FOREIGN KEY (memo_id) REFERENCES memos (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE memo_attachments
    DROP CONSTRAINT fk_memo_attachments_memo,
    ADD CONSTRAINT fk_memo_attachments_memo
        FOREIGN KEY (memo_id) REFERENCES memos (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE record_answers
    DROP CONSTRAINT fk_record_answers_record,
    ADD CONSTRAINT fk_record_answers_record
        FOREIGN KEY (record_id) REFERENCES records (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE record_memos
    DROP CONSTRAINT fk_record_memos_record,
    DROP CONSTRAINT fk_record_memos_memo,
    ADD CONSTRAINT fk_record_memos_record
        FOREIGN KEY (record_id) REFERENCES records (id) ON DELETE CASCADE NOT VALID,
    ADD CONSTRAINT fk_record_memos_memo
        FOREIGN KEY (memo_id) REFERENCES memos (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE record_competency_tags
    DROP CONSTRAINT fk_record_competency_tags_record,
    ADD CONSTRAINT fk_record_competency_tags_record
        FOREIGN KEY (record_id) REFERENCES records (id) ON DELETE CASCADE NOT VALID;

ALTER TABLE record_embeddings
    DROP CONSTRAINT fk_record_embeddings_record,
    ADD CONSTRAINT fk_record_embeddings_record
        FOREIGN KEY (record_id) REFERENCES records (id) ON DELETE CASCADE NOT VALID;
