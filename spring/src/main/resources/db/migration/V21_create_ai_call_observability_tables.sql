-- AI 요청 단위의 사용량, 비용 및 성능 정보를 저장한다.
--
-- ai_requests: 사용자 행동으로 시작된 논리적인 AI 요청 1건
-- ai_call_attempts: 최초 호출과 재시도를 포함한 실제 AI 공급자 호출 1건

CREATE TABLE ai_requests (
    id uuid NOT NULL,
    request_id uuid NOT NULL,
    user_id uuid,
    feature varchar(50) NOT NULL,
    final_status varchar(30) NOT NULL,
    retry_count integer NOT NULL DEFAULT 0,
    total_latency_ms bigint,
    total_estimated_cost_usd numeric(18, 10),
    created_at timestamp(6) NOT NULL,
    completed_at timestamp(6),

    PRIMARY KEY (id),

    CONSTRAINT uk_ai_requests_request_id
        UNIQUE (request_id),

    CONSTRAINT fk_ai_requests_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_ai_requests_feature
        CHECK (
            feature IN (
                'RECORD_EMBEDDING',
                'RECORD_ANALYSIS',
                'QUESTION_EMBEDDING',
                'INSIGHT_RECOMMENDATION'
            )
        ),

    CONSTRAINT ck_ai_requests_final_status
        CHECK (
            final_status IN (
                'PENDING',
                'SUCCESS',
                'FAILED'
            )
        ),

    CONSTRAINT ck_ai_requests_retry_count
        CHECK (retry_count >= 0),

    CONSTRAINT ck_ai_requests_total_latency
        CHECK (
            total_latency_ms IS NULL
            OR total_latency_ms >= 0
        ),

    CONSTRAINT ck_ai_requests_total_cost
        CHECK (
            total_estimated_cost_usd IS NULL
            OR total_estimated_cost_usd >= 0
        ),

    CONSTRAINT ck_ai_requests_completed_at
        CHECK (
            completed_at IS NULL
            OR completed_at >= created_at
        )
);

CREATE TABLE ai_call_attempts (
    id uuid NOT NULL,
    ai_request_id uuid NOT NULL,
    attempt_number integer NOT NULL,
    provider varchar(50) NOT NULL,
    model_id varchar(255) NOT NULL,
    input_tokens bigint,
    output_tokens bigint,
    estimated_cost_usd numeric(18, 10),
    latency_ms bigint,
    status varchar(30) NOT NULL,
    error_code varchar(100),
    created_at timestamp(6) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_ai_call_attempts_request
        FOREIGN KEY (ai_request_id)
        REFERENCES ai_requests (id)
        ON DELETE CASCADE,

    CONSTRAINT uk_ai_call_attempts_request_attempt
        UNIQUE (ai_request_id, attempt_number),

    CONSTRAINT ck_ai_call_attempts_attempt_number
        CHECK (attempt_number >= 1),

    CONSTRAINT ck_ai_call_attempts_input_tokens
        CHECK (
            input_tokens IS NULL
            OR input_tokens >= 0
        ),

    CONSTRAINT ck_ai_call_attempts_output_tokens
        CHECK (
            output_tokens IS NULL
            OR output_tokens >= 0
        ),

    CONSTRAINT ck_ai_call_attempts_cost
        CHECK (
            estimated_cost_usd IS NULL
            OR estimated_cost_usd >= 0
        ),

    CONSTRAINT ck_ai_call_attempts_latency
        CHECK (
            latency_ms IS NULL
            OR latency_ms >= 0
        ),

    CONSTRAINT ck_ai_call_attempts_status
        CHECK (
            status IN (
                'SUCCESS',
                'TIMEOUT',
                'RATE_LIMITED',
                'PROVIDER_ERROR',
                'INVALID_RESPONSE',
                'UNKNOWN_ERROR'
            )
        )
);

-- 사용자별 일간/월간 비용 및 사용량 조회
CREATE INDEX idx_ai_requests_user_created_at
    ON ai_requests (user_id, created_at);

-- 기능별 호출 수, 성공률 및 지연 시간 조회
CREATE INDEX idx_ai_requests_feature_created_at
    ON ai_requests (feature, created_at);

-- 최종 성공/실패율 조회
CREATE INDEX idx_ai_requests_status_created_at
    ON ai_requests (final_status, created_at);

-- 공금자/모델별 토큰 및 비용 조회
CREATE INDEX idx_ai_call_attempts_provider_model_created_at
    ON ai_call_attempts (provider, model_id, created_at);

-- 공급자 오류율 조회
CREATE INDEX idx_ai_call_attempts_status_created_at
    ON ai_call_attempts (status, created_at);