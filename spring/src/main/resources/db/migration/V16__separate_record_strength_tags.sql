CREATE TABLE strength_tags (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    code varchar(50) NOT NULL,
    name varchar(100) NOT NULL,
    description varchar(255) NOT NULL,
    evaluation_criteria varchar(255) NOT NULL,
    positive_example varchar(500) NOT NULL,
    negative_example varchar(500) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_strength_tags_code UNIQUE (code)
);

CREATE TABLE strength_tag_embeddings (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    strength_tag_id uuid NOT NULL,
    embedding_model varchar(100) NOT NULL,
    embedding vector(3072) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_strength_tag_embedding
        UNIQUE (strength_tag_id, embedding_model),
    CONSTRAINT fk_strength_tag_embeddings_strength_tag
        FOREIGN KEY (strength_tag_id) REFERENCES strength_tags ON DELETE CASCADE
);

CREATE TABLE record_strength_tags (
    id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    cosine_similarity real NOT NULL,
    strength_tag_id uuid NOT NULL,
    record_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_record_strength_tags_cosine_similarity
        CHECK (cosine_similarity >= -1.0 AND cosine_similarity <= 1.0),
    CONSTRAINT uk_record_strength_tag UNIQUE (record_id, strength_tag_id),
    CONSTRAINT fk_record_strength_tags_strength_tag
        FOREIGN KEY (strength_tag_id) REFERENCES strength_tags ON DELETE RESTRICT,
    CONSTRAINT fk_record_strength_tags_record
        FOREIGN KEY (record_id) REFERENCES records ON DELETE CASCADE
);

CREATE INDEX idx_record_strength_tags_strength_tag
    ON record_strength_tags (strength_tag_id);
