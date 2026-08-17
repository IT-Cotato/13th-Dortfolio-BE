CREATE TABLE strength_tags (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    code varchar(50) NOT NULL,
    name varchar(100) NOT NULL,
    description varchar(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_strength_tags_code UNIQUE (code)
);

CREATE TABLE record_strength_tags (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    score real NOT NULL,
    strength_tag_id uuid NOT NULL,
    record_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_record_strength_tags_score CHECK (score >= 0.0 AND score <= 1.0),
    CONSTRAINT uk_record_strength_tag UNIQUE (record_id, strength_tag_id),
    CONSTRAINT fk_record_strength_tags_strength_tag
        FOREIGN KEY (strength_tag_id) REFERENCES strength_tags ON DELETE RESTRICT,
    CONSTRAINT fk_record_strength_tags_record
        FOREIGN KEY (record_id) REFERENCES records ON DELETE CASCADE
);

CREATE INDEX idx_record_strength_tags_strength_tag
    ON record_strength_tags (strength_tag_id);
