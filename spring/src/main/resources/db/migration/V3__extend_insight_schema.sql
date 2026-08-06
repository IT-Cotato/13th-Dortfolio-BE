-- PR #41의 V2 마이그레이션 적용 이후 Insight 스키마를 확장

-- 컬럼들을 NULL 허용(Nullable) 상태로 추가
ALTER TABLE insights
    ADD COLUMN job_id_snapshot uuid,
    ADD COLUMN job_name_snapshot varchar(255),
    ADD COLUMN record_snapshot_at timestamp(6),
    ADD COLUMN base_completed_record_count integer,
    ADD COLUMN requested_at timestamp(6),
    ADD COLUMN failure_code varchar(100),
    ADD COLUMN failure_message text;

-- 데이터 백필(Backfill)
DELETE FROM insights
WHERE (status = 'PENDING' OR status = 'FAILED')
  AND job_id_snapshot IS NULL;

-- 기존 COMPLETED 데이터의 필수 시점 및 기본값 보정
UPDATE insights
SET requested_at = COALESCE(requested_at, created_at),
    record_snapshot_at = COALESCE(record_snapshot_at, created_at),
    base_completed_record_count = COALESCE(base_completed_record_count, 0),
    job_name_snapshot = COALESCE(job_name_snapshot, '기본 직무')
WHERE requested_at IS NULL
   OR record_snapshot_at IS NULL
   OR base_completed_record_count IS NULL
   OR job_name_snapshot IS NULL;

-- 필수 컬럼들에 NOT NULL 제약조건 적용
ALTER TABLE insights
    ALTER COLUMN job_id_snapshot SET NOT NULL,
ALTER COLUMN job_name_snapshot SET NOT NULL,
    ALTER COLUMN record_snapshot_at SET NOT NULL,
    ALTER COLUMN base_completed_record_count SET NOT NULL,
    ALTER COLUMN requested_at SET NOT NULL;


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

-- 유니크 인덱스 생성 전, 중복 PENDING 레코드 정리
DELETE FROM insights i1
    USING insights i2
WHERE i1.user_id = i2.user_id
  AND i1.status = 'PENDING'
  AND i2.status = 'PENDING'
  AND i1.created_at < i2.created_at;

-- 사용자당 PENDING 상태의 인사이트 중복 생성을 막는 유니크 인덱스
CREATE UNIQUE INDEX uq_insights_user_pending
    ON insights (user_id)
    WHERE status = 'PENDING';