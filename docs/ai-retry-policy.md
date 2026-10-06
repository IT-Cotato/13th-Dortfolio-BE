# AI 실패·재시도 정책

운영 개선안 4.5의 오류 구분, 제한된 자동 재시도, 상태 조회와 대기열 측정을 구현한다. 기준은 develop의 프롬프트 버전 관리 변경 이후이며, 미머지 PR #130의 관측성 구현은 포함하지 않는다. circuit breaker와 운영 알림 연결은 후속 작업이다.

## 재시도 책임과 횟수

Spring이 재시도를 관리하고 FastAPI의 Gemini SDK는 각 요청을 한 번만 실행한다. 기록 분석은 임베딩과 생성 호출을 각각 최대 3회 시도한다. 인사이트 직무 역량 추천도 최대 3회 시도한다. 총 3회는 최초 호출 1회와 재시도 2회를 뜻한다.

기록 분석은 한 작업 안에서 임베딩 호출이 성공하면 그 결과를 메모리에 유지한다. 생성 호출을 재시도할 때는 임베딩과 후보 검색을 다시 실행하지 않는다. 두 단계가 각각 세 번째 시도에서 성공하면 공급자 호출은 총 6회이다. 단계별 자동 시도 횟수는 작업 큐의 실행 횟수와 다른 값이다.

프로세스 재시작이나 사용자의 별도 재시도 요청에서는 임베딩부터 다시 실행한다. 임베딩 중간 결과의 DB 저장·재사용은 이번 작업에 포함하지 않는다. 기록 큐의 기존 소유권·세대 번호·lease 복구도 유지한다. 예상하지 못한 작업자/DB 장애의 큐 복구는 공급자 호출의 단계별 재시도와 별개이다.

## 실패 분류

FastAPI의 오류 응답은 `status`, `errorCode`, 안전한 `message`를 포함한다. 공급자의 Retry-After 헤더가 있으면 전달한다. Spring은 PR #130의 같은 필드가 있는 응답과 FastAPI HTTPException의 detail 안에 들어간 형태도 처리한다. 기존 메타데이터 없는 HTTP 오류는 상태 코드로 판단하며, 알려진 API 키 누락 응답은 설정 오류로 판단한다.

| 원인 | 오류 응답 예 | 자동 재시도 |
|---|---|---|
| 요청 과다 | `RATE_LIMITED`, `HTTP_429` | 허용 |
| timeout | `TIMEOUT`, `TIMEOUT` 또는 `HTTP_408`/`HTTP_504` | 허용 |
| 연결 오류 | `PROVIDER_ERROR`, `CONNECTION_ERROR` | 허용 |
| 공급자 일시 장애 | HTTP 500·502·503·504 | 허용 |
| API 키 누락·인증 오류 | `CONFIGURATION_ERROR` | 중단 |
| JSON·후보 ID·스키마 검증 실패 | `INVALID_RESPONSE`, `SCHEMA_VALIDATION_FAILED` | 중단 |
| 출력 상한 도달 | `OUTPUT_LIMIT`, `OUTPUT_TOKEN_LIMIT` | 중단 |
| 그 외 요청 거절·지원되지 않는 기능 | HTTP 400·422·501 등 | 중단 |

검증 실패와 출력 상한은 422로 반환한다. 공급자 HTTP 상태는 보존하되, 응답이 INVALID_RESPONSE 또는 CONFIGURATION_ERROR이면 502·503이더라도 재시도하지 않는다. 오류 응답에는 원문·검증 예외 전문을 넣지 않는다. Spring의 기록 분석 실패 로그에도 공급자 응답 본문을 남기지 않는다.

## 대기와 설정

기본 대기는 첫 실패 후 2초, 두 번째 실패 후 4초에 각각 0~50% jitter를 더한 값이다. Retry-After는 초 단위와 RFC 1123 날짜를 지원한다. 자체 대기 시간과 Retry-After 중 더 긴 시간 이후에 다음 요청을 시작한다. 음수·과거·형식이 잘못된 값은 자체 백오프로 처리한다. 마지막 시도 후에는 기다리지 않는다. 스레드가 중단되면 추가 호출을 중단하고 interrupt 상태를 유지한다.

| 설정 | 기본값 | 범위 |
|---|---|---|
| `AI_RETRY_MAX_ATTEMPTS` | 3 | 기록 임베딩·생성 각각의 총 시도 횟수 |
| `AI_RETRY_INITIAL_BACKOFF` | 2s | 기록의 초기 대기 |
| `INSIGHT_RECOMMENDATION_MAX_ATTEMPTS` | 3 | 인사이트 추천의 총 시도 횟수 |
| `INSIGHT_RECOMMENDATION_INITIAL_BACKOFF` | 2s | 인사이트의 초기 대기 |

정책은 ConfigurationProperties로 관리하며 운영 Compose에 전달한다. FastAPI에는 SDK `attempts=1`을 명시한다. 환경 변수를 변경한 경우 해당 서비스를 재시작해야 한다.

이번 구현은 기존 비동기 작업 스레드에서 재시도 시간을 기다린다. 긴 Retry-After가 오면 그 시간 동안 실행 슬롯을 점유한다. 기록 작업의 lease heartbeat는 계속 유지한다. 인사이트는 현재 프로세스에서 실제로 실행 중인 작업을 stale 복구 대상에서 제외하므로, 긴 대기만으로 10분 이후 실패 처리되지 않는다. 이 실행 추적은 현재 단일 Spring 인스턴스 기준이며 여러 인스턴스로 확장할 때는 공유 heartbeat lease가 필요하다. 장시간 대기를 DB의 다음 실행 시각으로 예약하는 구조는 후속 개선이다.

## 사용자 상태

### 기록

`GET /api/records/{recordId}/analysis`는 본인 소유의 활성 기록에 대해서만 다음 정보를 반환한다. 삭제한 기록과 삭제한 활동의 기록은 조회하지 않는다.

- `recordId`
- `status`: NOT_REQUESTED, PENDING, RUNNING, COMPLETED, FAILED
- `failureCode`, `failureMessage`
- `failureRetryable`

현재 세대의 작업자가 실행 중일 때만 RUNNING을 반환한다. 자동 재시도를 기다리는 중에도 RUNNING이다. 기록 상태 COMPLETED는 사용자가 작성을 완료했다는 뜻이며 AI 분석의 COMPLETED와 구분한다.

기존 `POST /api/records/analysis/retry-failed`는 해당 사용자의 재시도 가능한 실패 기록을 다시 예약한다. 자동 시도 횟수를 소진했어도 일시적인 실패는 재시도 가능 상태를 유지한다. 설정·검증·출력 상한 오류는 이 재시도 대상에서 제외한다. 입력을 바꿔 새 분석을 요청하는 것은 별도 작업이다.

### 인사이트

기존 `GET /api/insights/generations/{generationId}`에 `failureRetryable`을 추가한다. FAILED 상태의 일시적인 AI 서비스 실패 코드 I002에만 true이다. 잘못된 응답은 I003, 설정 오류는 I010, 요청 거절은 I011, 출력 상한은 I012로 구분한다.

이 값은 실패 원인이 재시도로 해결될 수 있는지를 뜻한다. 지금 새 생성 요청을 할 수 있는지는 기존 `/api/insights/eligibility`의 `eligible`, `nextAvailableAt`으로 판단한다. 기존 쿨다운과 기록 수 조건은 변경하지 않는다. 과거의 I002 결과는 당시 상세 원인이 저장되지 않아 현재 코드 기준으로 해석한다.

## 지표와 검증

기존 Micrometer 레지스트리에 다음 지표를 등록한다.

- `ai.call.attempts`: record_embedding, record_analysis, insight_recommendation 단계별 Spring 호출 시도 수
- `ai.call.retries`: 단계별 자동 재시도 대기 진입 수
- `ai.record.queue.waiting`: 현재 세대의 활성 기록 분석 READY 작업 수
- `ai.record.queue.oldest.wait.seconds`: 그중 가장 오래 기다린 현재 요청의 대기 시간. 비어 있으면 0

지표에는 사용자·기록 ID를 태그로 붙이지 않는다. 대기 시간은 작업 행 최초 생성 시각이 아닌 현재 분석 요청 시각으로 계산한다. 재시도 중인 RUNNING 작업은 대기열 수에 포함하지 않는다. 인사이트는 별도 영속 작업 큐가 없으므로 이 대기열 지표는 기록 큐에만 해당한다.

운영 Actuator의 웹 노출은 기존 health만 유지한다. 지표는 레지스트리에 수집되며 외부 모니터링 시스템 연결과 알림 임계값은 후속 설정이다. PR #130의 호출 이력·비용 저장과는 다른 지표이다.

검증에는 오류 분류, 최대 시도 수, Retry-After 두 형식, 중단 처리, 생성 재시도 중 임베딩 재호출 방지, 사용자 재시도 가능 상태, 상태 조회 권한·세대 구분, 대기열 쿼리와 stale 복구를 포함한다. 실제 Gemini 공급자 요청은 테스트에서 실행하지 않는다.
