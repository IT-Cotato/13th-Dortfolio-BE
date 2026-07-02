# 13th-Dortfolio-BE

## 로컬 개발 환경 세팅

### 사전 요구사항
- Docker / Docker Compose
- JDK 17
- (선택) Python 3.13 — FastAPI 로컬 직접 실행 시

### 실행 순서

1. 환경 변수 파일 생성
   ```bash
   cp .env.example .env
   ```
   필요 시 `.env` 값(DB 이름/계정, GEMINI_API_KEY 등) 수정

2. DB / FastAPI 컨테이너 기동
   ```bash
   docker compose -f docker-compose.local.yml up -d
   ```
   - postgres: `pgvector/pgvector:pg16` 이미지, `127.0.0.1:5432`로만 바인딩(외부 네트워크에서 접근 불가)
   - fastapi: 현재 `/health`만 있는 최소 스텁 (AI 기능은 추후 구현)

3. 컨테이너 상태 확인
   ```bash
   docker compose -f docker-compose.local.yml ps
   curl http://localhost:8000/health
   ```

4. Spring 서버 실행 (local profile)
   ```bash
   cd spring
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```
   IDE에서 실행할 경우 Active Profiles에 `local` 추가

5. 정상 기동 확인
   ```bash
   curl http://localhost:8080/actuator/health
   ```

### 종료 / 초기화

```bash
# 컨테이너 종료
docker compose -f docker-compose.local.yml down

# DB 데이터까지 완전 초기화하고 싶을 때
docker compose -f docker-compose.local.yml down -v
```

### 참고
- `spring/compose.yaml`은 Spring Boot Docker Compose 플러그인이 자동 인식하는 파일이며, local profile에서는 `spring.docker.compose.enabled=false`로 꺼두었으므로 `docker-compose.local.yml`과 충돌하지 않습니다.
- pgvector 확장은 `infra/postgres/init/001-init-extensions.sql`에서 컨테이너 최초 생성 시 자동으로 설치됩니다.
