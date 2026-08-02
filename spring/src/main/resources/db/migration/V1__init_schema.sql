-- 초기 스키마.
-- 기존 ddl-auto가 생성하던 스키마를 그대로 옮겨온 것이며,
-- 긴 텍스트 컬럼은 oid(large object)가 아닌 text로 정의한다.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    email varchar(100) NOT NULL,
    nickname varchar(50) NOT NULL,
    password varchar(255),
    profile_image_url varchar(255),
    provider varchar(50),
    provider_id varchar(255),
    role varchar(255) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE jobs (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    description varchar(255),
    name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE competency_tags (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    description varchar(255),
    name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE job_competencies (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    sort_order integer NOT NULL,
    competency_tag_id uuid NOT NULL,
    job_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_job_competency UNIQUE (job_id, competency_tag_id),
    CONSTRAINT fk_job_competencies_tag FOREIGN KEY (competency_tag_id) REFERENCES competency_tags,
    CONSTRAINT fk_job_competencies_job FOREIGN KEY (job_id) REFERENCES jobs
);

CREATE TABLE user_jobs (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    is_primary boolean NOT NULL,
    job_id uuid NOT NULL,
    user_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_job UNIQUE (user_id, job_id),
    CONSTRAINT fk_user_jobs_job FOREIGN KEY (job_id) REFERENCES jobs,
    CONSTRAINT fk_user_jobs_user FOREIGN KEY (user_id) REFERENCES users
);

CREATE TABLE user_term_agreements (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    agreed boolean NOT NULL,
    agreed_at timestamp(6) NOT NULL,
    term_type varchar(255) NOT NULL,
    user_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_term_agreements_user FOREIGN KEY (user_id) REFERENCES users
);

CREATE TABLE password_reset_tokens (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    expires_at timestamp(6) NOT NULL,
    token_hash varchar(255) NOT NULL,
    used_at timestamp(6),
    user_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users
);

CREATE TABLE activity_type (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    is_default boolean NOT NULL,
    name varchar(255) NOT NULL,
    user_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_activity_type_user FOREIGN KEY (user_id) REFERENCES users
);

CREATE TABLE activity (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    archived_at timestamp(6),
    delete_pending_until timestamp(6),
    deleted_at timestamp(6),
    description varchar(255),
    ended_at date,
    is_ongoing boolean NOT NULL,
    started_at date NOT NULL,
    status varchar(255) NOT NULL CHECK (status IN ('IN_PROGRESS', 'ARCHIVED')),
    title varchar(255) NOT NULL,
    activity_type_id uuid NOT NULL,
    user_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_activity_type FOREIGN KEY (activity_type_id) REFERENCES activity_type,
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users
);

CREATE TABLE templates (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    builtin_code varchar(50),
    builtin_version integer,
    deleted_at timestamp(6),
    description varchar(50),
    is_builtin boolean NOT NULL,
    title varchar(20) NOT NULL,
    user_id uuid,
    PRIMARY KEY (id),
    CONSTRAINT uk_templates_builtin_code UNIQUE (builtin_code),
    CONSTRAINT fk_templates_user FOREIGN KEY (user_id) REFERENCES users
);

CREATE TABLE template_questions (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    builtin_code varchar(80),
    deleted_at timestamp(6),
    description varchar(100),
    question_text varchar(30) NOT NULL,
    required boolean NOT NULL,
    sort_order integer NOT NULL,
    template_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_template_questions_builtin_code UNIQUE (builtin_code),
    CONSTRAINT fk_template_questions_template FOREIGN KEY (template_id) REFERENCES templates
);

CREATE TABLE activity_templates (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    sort_order integer NOT NULL,
    activity_id uuid NOT NULL,
    template_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_activity_template UNIQUE (activity_id, template_id),
    CONSTRAINT fk_activity_templates_activity FOREIGN KEY (activity_id) REFERENCES activity,
    CONSTRAINT fk_activity_templates_template FOREIGN KEY (template_id) REFERENCES templates
);

CREATE TABLE memos (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    color varchar(255),
    content text NOT NULL,
    delete_pending_until timestamp(6),
    deleted_at timestamp(6),
    expires_at timestamp(6),
    is_important boolean NOT NULL,
    sort_order integer NOT NULL,
    title varchar(255),
    use_count integer NOT NULL,
    activity_id uuid,
    user_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_memos_activity FOREIGN KEY (activity_id) REFERENCES activity,
    CONSTRAINT fk_memos_user FOREIGN KEY (user_id) REFERENCES users
);

CREATE TABLE memo_images (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    image_url varchar(255) NOT NULL,
    s3_key varchar(255),
    sort_order integer NOT NULL,
    memo_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_memo_images_memo FOREIGN KEY (memo_id) REFERENCES memos
);

CREATE TABLE memo_attachments (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    file_size integer NOT NULL,
    file_type varchar(255) NOT NULL,
    file_url varchar(255) NOT NULL,
    sort_order integer NOT NULL,
    memo_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_memo_attachments_memo FOREIGN KEY (memo_id) REFERENCES memos
);

CREATE TABLE records (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    completed_at timestamp(6),
    delete_pending_until timestamp(6),
    deleted_at timestamp(6),
    status varchar(255) NOT NULL CHECK (status IN ('DRAFT', 'COMPLETED')),
    title varchar(255) NOT NULL,
    version bigint,
    activity_id uuid NOT NULL,
    template_id uuid NOT NULL,
    user_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_records_activity FOREIGN KEY (activity_id) REFERENCES activity,
    CONSTRAINT fk_records_template FOREIGN KEY (template_id) REFERENCES templates,
    CONSTRAINT fk_records_user FOREIGN KEY (user_id) REFERENCES users
);

CREATE TABLE record_answers (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    answer_text text NOT NULL,
    question_description varchar(100),
    question_text varchar(30) NOT NULL,
    required boolean NOT NULL,
    sort_order integer NOT NULL,
    template_question_id uuid NOT NULL,
    record_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_record_answer_question UNIQUE (record_id, template_question_id),
    CONSTRAINT fk_record_answers_record FOREIGN KEY (record_id) REFERENCES records
);

CREATE TABLE record_memos (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    is_collapsed boolean NOT NULL,
    sort_order integer NOT NULL,
    memo_id uuid NOT NULL,
    record_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_record_memo UNIQUE (record_id, memo_id),
    CONSTRAINT fk_record_memos_memo FOREIGN KEY (memo_id) REFERENCES memos,
    CONSTRAINT fk_record_memos_record FOREIGN KEY (record_id) REFERENCES records
);

CREATE TABLE record_competency_tags (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    score real NOT NULL,
    competency_tag_id uuid NOT NULL,
    record_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_record_competency_tag UNIQUE (record_id, competency_tag_id),
    CONSTRAINT fk_record_competency_tags_tag FOREIGN KEY (competency_tag_id) REFERENCES competency_tags,
    CONSTRAINT fk_record_competency_tags_record FOREIGN KEY (record_id) REFERENCES records
);

CREATE TABLE record_embeddings (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    embedding vector NOT NULL,
    embedding_model varchar(255) NOT NULL,
    record_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_record_embeddings_record FOREIGN KEY (record_id) REFERENCES records
);

CREATE TABLE record_analysis (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    ai_analysis_status varchar(255) NOT NULL CHECK (ai_analysis_status IN ('PENDING', 'COMPLETED', 'FAILED')),
    analyzed_at timestamp(6),
    analyzed_record_updated_at timestamp(6),
    evidence_snippets text,
    failure_reason text,
    failure_retryable boolean NOT NULL,
    last_attempt_failed boolean NOT NULL,
    last_attempted_at timestamp(6),
    last_failure_reason text,
    last_failure_retryable boolean NOT NULL,
    summary text,
    record_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_record_analysis_record UNIQUE (record_id),
    CONSTRAINT fk_record_analysis_record FOREIGN KEY (record_id) REFERENCES records ON DELETE CASCADE
);

CREATE TABLE ai_insights (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    content text NOT NULL,
    insight_date date NOT NULL,
    insight_type varchar(255) NOT NULL,
    refreshed_at timestamp(6) NOT NULL,
    user_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_insights_user FOREIGN KEY (user_id) REFERENCES users
);

CREATE TABLE insights (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    status varchar(255) NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    completed_at timestamp(6),
    failed_at timestamp(6),
    user_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_insights_user FOREIGN KEY (user_id) REFERENCES users
);

-- 검색 성능 (기록 제목/내용 부분 일치)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_records_title_trgm ON records USING gin (lower(title) gin_trgm_ops);
CREATE INDEX idx_record_answers_text_trgm ON record_answers USING gin (lower(answer_text) gin_trgm_ops);
