# AI 프롬프트 버전과 생성 제한

기록 분석과 직무 역량 추천은 FastAPI의 버전별 템플릿을 사용한다. 강점 후보 검색 상한과 적합·부적합 예시는 기존 정책을 유지한다. 개인정보 마스킹은 후속 작업이다.

## 프롬프트 변경

- 템플릿: `fastapi/app/prompts/record_analysis/v1.txt`, `fastapi/app/prompts/insight_recommendation/v1.txt`
- 활성 버전과 스키마 버전: `fastapi/app/prompts/registry.py`
- 변수 치환은 Python 표준 라이브러리 `string.Template`을 사용한다. 삽입한 사용자 내용은 다시 치환하지 않는다. 템플릿에서 리터럴 `$`는 `$$`로 작성한다.
- 배포한 파일은 보존한다. 지시문·판단 방식·입력 배치를 변경하면 `v2.txt`를 추가하고 해당 기능의 레지스트리 버전을 바꾼다.
- 출력 필드·타입·필수 조건·검증 계약을 변경하면 Pydantic 출력 모델과 관련 Spring 처리 코드를 수정하고 `schema_version`도 올린다. 지시문만 변경한 경우 스키마는 v1을 유지할 수 있다.
- 이전 버전으로 되돌릴 때는 그 버전에 맞는 출력 모델·검증 코드도 함께 확인한다. 기능별 비교를 위해 모델, 후보 설정, 평가 입력은 고정한다.
- 버전 변경만으로 기존 결과를 자동 재분석하지 않는다. 현재 DB에는 각 기록의 최신 결과만 남으며 모든 과거 실행 이력을 보관하지 않는다.

## 응답과 저장

Gemini에는 `RecordAnalysisOutput` 또는 `InsightRecommendationOutput` 스키마만 전달한다. 버전 정보는 Gemini가 생성하지 않는다. 검증에 성공한 뒤 FastAPI가 다음 `metadata`를 응답에 붙인다.

```json
{
  "promptVersion": "record_analysis.v1",
  "schemaVersion": "record_analysis.v1",
  "model": "gemini-3.6-flash",
  "maxOutputTokens": 8192,
  "thinkingLevel": null
}
```

`model`은 실제 요청에 사용한 모델 ID이다. 모델 별칭의 정확한 공급자 내부 버전을 고정하는 값은 아니다. `thinkingLevel: null`은 설정을 보내지 않고 모델 기본 동작을 사용했다는 뜻이다.

Spring은 이 정보를 `record_analysis`와 `insights`의 `prompt_version`, `schema_version`, `generation_model`, `max_output_tokens`, `thinking_level` 컬럼에 결과와 같은 트랜잭션으로 저장한다. 인사이트 메타데이터는 AI 직무 역량 추천 단계의 정보이며, 코드로 계산하는 강점·템플릿 통계의 버전을 뜻하지 않는다.

V21 마이그레이션은 기존 결과를 수정하지 않고 nullable 컬럼을 추가한다. 과거 결과는 버전을 확인할 수 없으므로 NULL을 유지한다. Spring은 배포 전환 중 이전 FastAPI가 반환하는 메타데이터 없는 응답도 처리할 수 있다. 새 FastAPI는 성공 응답에 항상 메타데이터를 포함한다. 기록이 대기·실패 상태로 전환되면 지워진 결과와 함께 메타데이터도 지운다. 사용자용 조회 API에 새 필드를 노출하지는 않는다.

프롬프트 버전만으로 당시 입력 전체를 재현할 수는 없다. DB의 강점 정의·판단 기준·예시, 후보와 사용자 기록은 별도 입력이다. 예시 수정 시 관련 강점 임베딩도 갱신해야 한다. 후보 내용 hash나 카탈로그 이력 관리는 이번 변경에 포함하지 않는다. 이후 같은 입력의 결과 재사용을 도입할 때 이 입력들과 생성 설정도 캐시 키에 반영해야 한다.

## 생성 설정

아래 환경 변수는 로컬·운영 Compose 모두 FastAPI에 전달한다.

| 환경 변수 | 기본값 | 용도 |
|---|---|---|
| `RECORD_ANALYSIS_MAX_OUTPUT_TOKENS` | `8192` | 기록 분석 생성 상한 |
| `INSIGHT_RECOMMENDATION_MAX_OUTPUT_TOKENS` | `8192` | 직무 역량 추천 생성 상한 |
| `RECORD_ANALYSIS_THINKING_LEVEL` | 빈 값 | 기록 분석 사고 수준 |
| `INSIGHT_RECOMMENDATION_THINKING_LEVEL` | 빈 값 | 직무 역량 추천 사고 수준 |

상한은 양의 정수여야 한다. 사고 수준은 빈 값 또는 `minimal`, `low`, `medium`, `high`를 받는다. 모델별 지원 값이 다르므로 실제 운영 모델의 지원 여부를 확인하고 설정한다. 이번 변경은 Gemini 3 계열의 사고 수준 설정을 대상으로 하며 Gemini 2.5의 `thinking_budget` 설정은 추가하지 않는다. 기존 SDK `google-genai==1.55.0`이 필요한 설정 필드를 제공하므로 의존성은 변경하지 않는다.

8192는 평가 전 시작값이며 정상 응답을 보장하는 최소값이나 비용 절감 효과가 검증된 값이 아니다. 기본 사고 수준을 유지한 상태에서 입력·출력·사고 토큰과 종료 사유를 로그로 측정하고, 같은 한국어 평가셋에서 상한과 사고 수준을 각각 비교한다. [Gemini 공식 안내](https://ai.google.dev/gemini-api/docs/generate-content/thinking?hl=en)에 따르면 출력 상한에는 사고 토큰도 포함된다. 작은 상한으로 사고를 억제하려고 하지 말고 사고 수준을 별도로 조정한다.

`MAX_TOKENS` 응답은 JSON이 파싱 가능하더라도 성공 결과로 저장하지 않는다. FastAPI는 422를 반환하고 Spring의 기존 정책이 자동 재시도를 중단한다. 이 경우 입력 길이와 출력·사고 토큰을 확인해 설정을 조정한다. 공급자 장애의 기존 재시도 정책은 유지한다. 로그에는 생성 결과 전문을 남기지 않는다.

PR에서는 동일한 평가 입력으로 JSON 성공 여부, 후보 외 ID, 근거 정확도, 입력·출력·사고 토큰, 지연을 비교한다. 프롬프트 v1 분리는 기존 렌더링 내용과 동일하며, 실제 Gemini 품질·비용 평가는 별도 운영 키와 평가셋으로 수행한다.
