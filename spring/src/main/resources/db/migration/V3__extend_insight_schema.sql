-- PR #41의 V2 마이그레이션 적용 이후 Insight 스키마를 확장한다.
ALTER TABLE insights
    ADD COLUMN job_id_snapshot uuid NOT NULL,
    ADD COLUMN job_name_snapshot varchar(255) NOT NULL,
    ADD COLUMN record_snapshot_at timestamp(6) NOT NULL,
    ADD COLUMN base_completed_record_count integer NOT NULL,
    ADD COLUMN requested_at timestamp(6) NOT NULL,
    ADD COLUMN failure_code varchar(100),
    ADD COLUMN failure_message text;


-- Insight 강점 통계 결과
CREATE TABLE insight_strengths
(
    id                       uuid         NOT NULL,
    created_at               timestamp(6) NOT NULL,
    updated_at               timestamp(6) NOT NULL,
    insight_id               uuid         NOT NULL,
    strength_tag_id_snapshot uuid         NOT NULL,
    strength_name_snapshot   varchar(255) NOT NULL,
    record_count             integer      NOT NULL,
    average_score            double precision NOT NULL,
    ratio                    double precision NOT NULL,
    rank                     integer      NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_insight_strength_tag
        UNIQUE (insight_id, strength_tag_id_snapshot),

    CONSTRAINT fk_insight_strengths_insight
        FOREIGN KEY (insight_id)
            REFERENCES insights (id)
            ON DELETE CASCADE
);


-- 강점별 대표 기록 스냅샷
CREATE TABLE insight_strength_records
(
    id                           uuid         NOT NULL,
    created_at                   timestamp(6) NOT NULL,
    updated_at                   timestamp(6) NOT NULL,
    insight_strength_id          uuid         NOT NULL,
    record_id_snapshot           uuid         NOT NULL,
    record_title_snapshot        varchar(255) NOT NULL,
    record_completed_at_snapshot timestamp(6) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_insight_strength_record
        UNIQUE (insight_strength_id, record_id_snapshot),

    CONSTRAINT fk_insight_strength_records_strength
        FOREIGN KEY (insight_strength_id)
            REFERENCES insight_strengths (id)
            ON DELETE CASCADE
);


-- Insight 템플릿 분포 결과
CREATE TABLE insight_template_statistics
(
    id                     uuid         NOT NULL,
    created_at             timestamp(6) NOT NULL,
    updated_at             timestamp(6) NOT NULL,
    insight_id             uuid         NOT NULL,
    template_id_snapshot   uuid         NOT NULL,
    template_name_snapshot varchar(255) NOT NULL,
    record_count           integer      NOT NULL,
    ratio                  double precision NOT NULL,
    rank                   integer      NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_insight_template_statistic
        UNIQUE (insight_id, template_id_snapshot),

    CONSTRAINT fk_insight_template_statistics_insight
        FOREIGN KEY (insight_id)
            REFERENCES insights (id)
            ON DELETE CASCADE
);


-- 직무 역량별 추천 기록 결과
CREATE TABLE insight_job_recommendations
(
    id                         uuid         NOT NULL,
    created_at                 timestamp(6) NOT NULL,
    updated_at                 timestamp(6) NOT NULL,
    insight_id                 uuid         NOT NULL,
    job_competency_id_snapshot uuid         NOT NULL,
    competency_name_snapshot   varchar(255) NOT NULL,
    record_id_snapshot         uuid         NOT NULL,
    record_title_snapshot      varchar(255) NOT NULL,
    template_name_snapshot     varchar(255) NOT NULL,
    reason                     text         NOT NULL,
    similarity                 double precision NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_insight_job_competency
        UNIQUE (insight_id, job_competency_id_snapshot),

    CONSTRAINT fk_insight_job_recommendations_insight
        FOREIGN KEY (insight_id)
            REFERENCES insights (id)
            ON DELETE CASCADE
);


-- 직무 역량별 임베딩
CREATE TABLE job_competency_embeddings
(
    id                uuid         NOT NULL,
    created_at        timestamp(6) NOT NULL,
    updated_at        timestamp(6) NOT NULL,
    job_competency_id uuid         NOT NULL,
    embedding_model   varchar(100) NOT NULL,
    embedding         vector(3072) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_job_competency_embedding
        UNIQUE (job_competency_id, embedding_model),

    CONSTRAINT fk_job_competency_embeddings_job_competency
        FOREIGN KEY (job_competency_id)
            REFERENCES job_competencies (id)
            ON DELETE CASCADE
);
