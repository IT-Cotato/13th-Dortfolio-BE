# 배포 가이드 (EC2 + GitHub Actions)

`develop`에 푸시하면 GitHub Actions가 테스트 → 이미지 빌드 → GHCR 푸시 → EC2 배포까지 자동으로 진행합니다.

```
GitHub(develop) → Actions(테스트/빌드) → GHCR(이미지) → EC2(docker compose)
                                                          ├─ nginx (80/443, HTTPS 종단)
                                                          │    └─ spring (127.0.0.1:8080)
                                                          ├─ certbot (인증서 자동 갱신)
                                                          ├─ postgres (pgvector)
                                                          ├─ redis
                                                          └─ fastapi
```

외부에 열린 포트는 nginx의 80/443뿐입니다. 나머지는 전부 컨테이너 네트워크 안에서만 통신합니다.

---

## 1. EC2 인스턴스 생성

AWS 콘솔 → EC2 → 인스턴스 시작

| 항목 | 값 |
|---|---|
| AMI | Ubuntu Server 22.04 LTS |
| 인스턴스 유형 | **t3.small 이상 권장** |
| 키 페어 | 새로 생성 후 `.pem` 파일 보관 (재발급 불가) |
| 스토리지 | 30GB |

> **t2.micro(프리티어)는 메모리 1GB라 Spring + Postgres + Redis + FastAPI를 동시에 띄우기 어렵습니다.**
> 프리티어로 가야 한다면 스왑 2GB를 잡고 시작하되, 컨테이너가 OOM으로 죽을 수 있습니다.

### 보안 그룹 (인바운드 규칙)

| 유형 | 포트 | 소스 | 용도 |
|---|---|---|---|
| SSH | 22 | 내 IP | 접속 |
| HTTP | 80 | 0.0.0.0/0 | HTTPS 리다이렉트 + 인증서 갱신 |
| HTTPS | 443 | 0.0.0.0/0 | API |

DB(5432)와 Redis(6379)는 **절대 열지 않습니다.** 컨테이너 네트워크 안에서만 통신하며,
외부에서 DB를 봐야 하면 SSH 터널을 씁니다.

```bash
ssh -i key.pem -L 5432:localhost:5432 ubuntu@<도메인>
```

Spring의 8080 포트도 외부에 열지 않습니다. compose가 `127.0.0.1:8080`에만 바인딩하므로
서버 안에서만 접근되며, 외부 요청은 전부 nginx(443)를 거칩니다.

---

## 1-2. 고정 IP (탄력적 IP)

인스턴스를 중지했다 켜면 퍼블릭 IP가 바뀌어 도메인·시크릿·OAuth 설정이 전부 어긋납니다.
도메인을 붙이기 전에 먼저 고정해야 합니다.

EC2 콘솔 → **탄력적 IP** → `탄력적 IP 주소 할당` → 생성된 주소 선택 → `작업` → `탄력적 IP 주소 연결` → 인스턴스 선택

> 탄력적 IP는 **실행 중인 인스턴스에 연결돼 있는 동안만 무료**입니다.
> 할당만 하고 방치하거나 인스턴스를 종료하면 시간당 요금이 붙으니, 프로젝트가 끝나면 반드시 릴리스하세요.

nip.io 도메인은 IP를 그대로 포함하므로(`<IP>.nip.io`), **IP가 바뀌면 도메인도 바뀌고 인증서를 다시 발급받아야 합니다.**
탄력적 IP를 먼저 붙여야 하는 이유입니다.

연결 후 바뀐 주소를 반영할 곳: GitHub `EC2_HOST` 시크릿, 서버 `.env`의 `DOMAIN`, 구글 OAuth 리다이렉트 URI.

---

## 2. EC2 초기 세팅

```bash
ssh -i key.pem ubuntu@<EC2_IP>
```

```bash
# Docker 설치
sudo apt update && sudo apt install -y docker.io docker-compose-v2 git
sudo usermod -aG docker ubuntu
newgrp docker    # 또는 재접속

# 스왑 2GB (메모리 여유가 부족한 인스턴스에서 권장)
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 저장소 클론
git clone https://github.com/IT-Cotato/13th-Dortfolio-BE.git
cd 13th-Dortfolio-BE

# 환경 변수 설정
cp .env.prod.example .env
nano .env        # 값 채우기
```

비밀 값 생성:
```bash
openssl rand -base64 64   # JWT_SECRET
openssl rand -hex 16      # JWT_SALT
```

---

## 3. GitHub Secrets 등록

저장소 → Settings → Secrets and variables → Actions → New repository secret

| 이름 | 값 |
|---|---|
| `EC2_HOST` | EC2 퍼블릭 IP |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | `.pem` 파일 **전체 내용** (`-----BEGIN ...` 포함) |
| `GHCR_TOKEN` | GitHub PAT (`read:packages` 권한) |
| `GHCR_USERNAME` | 위 PAT을 발급한 GitHub 계정명 |

`GHCR_TOKEN`은 Settings → Developer settings → Personal access tokens → Tokens (classic)에서
`read:packages` 권한으로 발급합니다. EC2가 GHCR에서 이미지를 받을 때 씁니다.

`GHCR_USERNAME`은 반드시 그 PAT을 발급한 계정이어야 합니다. 푸시한 사람 기준으로 로그인하면
토큰 소유자와 어긋나 배포가 실패할 수 있습니다.

> GHCR에 처음 이미지를 올리면 패키지가 **private**으로 생성됩니다.
> EC2에서 pull이 403으로 실패하면 저장소 → Packages → 해당 패키지 → Package settings에서
> 저장소와 연결(Manage Actions access)되어 있는지 확인하세요.

---

## 3-1. S3 버킷 (이미지 업로드)

메모 활동 사진과 프로필 이미지를 S3에 올립니다. 파일은 서버를 거치지 않고
클라이언트가 presigned URL로 직접 주고받습니다.

### 버킷 생성

S3 콘솔 → 버킷 만들기

| 항목 | 값 |
|---|---|
| 이름 | `dotfolio-images` |
| 리전 | 아시아 태평양(서울) ap-northeast-2 |
| 퍼블릭 액세스 차단 | **모두 차단 (기본값 유지)** |

버킷은 비공개입니다. 조회도 만료 시간이 있는 presigned URL로만 하므로 공개할 필요가 없습니다.

### CORS 설정

브라우저가 presigned URL로 직접 업로드하므로 버킷에 CORS를 열어야 합니다.
버킷 → 권한 → CORS(교차 출처 리소스 공유) → 편집

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["PUT", "GET"],
    "AllowedOrigins": [
      "https://dotfolio-theta.vercel.app",
      "http://localhost:5173",
      "http://localhost:3000"
    ],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3000
  }
]
```

### IAM 사용자와 액세스 키

IAM → 사용자 → 사용자 생성 (이름 `dotfolio-s3`) → 콘솔 액세스는 주지 않음

권한은 인라인 정책으로 이 버킷에만 붙입니다. `AmazonS3FullAccess` 같은 광범위한 정책은
키가 유출되면 계정의 모든 버킷이 노출되므로 쓰지 않습니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
      "Resource": "arn:aws:s3:::dotfolio-images/*"
    }
  ]
}
```

사용자 → 보안 자격 증명 → 액세스 키 만들기 → 사용 사례 `애플리케이션` 선택.
발급된 키는 **한 번만 보여주므로** 바로 복사해 `.env`의 `AWS_ACCESS_KEY_ID`,
`AWS_SECRET_ACCESS_KEY`에 넣습니다.

> 액세스 키는 절대 커밋하지 않습니다. 실수로 올라가면 GitHub이 감지해 AWS에 통보하지만,
> 그 전에 도용될 수 있으므로 즉시 비활성화하고 새로 발급해야 합니다.

---

## 3-2. 도메인과 HTTPS

Let's Encrypt는 IP 주소로는 인증서를 발급하지 않으므로 도메인이 필요합니다.

### 도메인 (nip.io)

nip.io는 `<IP>.nip.io` 형태의 이름을 그대로 그 IP로 응답해주는 서비스입니다. 가입도 설정도 없습니다.
탄력적 IP가 `3.27.226.133`이면 도메인은 `3.27.226.133.nip.io`입니다.

확인:
```bash
dig +short 3.27.226.133.nip.io     # 같은 IP가 나와야 합니다
```

> **주의: nip.io는 Public Suffix List에 없어서 모든 사용자가 Let's Encrypt 주간 발급 한도(50장)를 공유합니다.**
> 최초 발급이나 갱신이 `too many certificates already issued for: nip.io`로 실패할 수 있습니다.
> 그런 경우 [DuckDNS](https://www.duckdns.org)에서 이름을 하나 받아 `.env`의 `DOMAIN`만 바꾸면 됩니다.
> 설정은 도메인에 종속적이지 않습니다.

### 인증서 최초 발급

nginx는 인증서 파일이 없으면 기동에 실패합니다. 그래서 최초 1회는 nginx 없이 발급받고,
이후 갱신만 certbot 컨테이너가 자동으로 처리합니다.

```bash
cd ~/13th-Dortfolio-BE

# .env에 도메인 추가
echo 'DOMAIN=3.27.226.133.nip.io' >> .env

# 80 포트를 비운다 (certbot이 직접 사용해야 함)
docker compose -f docker-compose.prod.yml down

docker run --rm -p 80:80 \
  -v dortfolio-prod_certbot-conf:/etc/letsencrypt \
  certbot/certbot certonly --standalone \
  -d 3.27.226.133.nip.io \
  --email dotfolio.cotato@gmail.com --agree-tos --no-eff-email

# 전체 기동 (nginx 포함)
docker compose -f docker-compose.prod.yml up -d
```

확인:
```bash
curl -I https://3.27.226.133.nip.io/swagger-ui.html
```

> Let's Encrypt는 같은 도메인에 **주당 5회**까지만 발급해줍니다.
> 명령이 실패하면 무작정 재시도하지 말고 오류 메시지부터 읽으세요.

### 발급 후 반영할 곳

| 위치 | 값 |
|---|---|
| GitHub `EC2_HOST` 시크릿 | 탄력적 IP (도메인도 가능) |
| 서버 `.env`의 `DOMAIN` | `3.27.226.133.nip.io` |
| 서버 `.env`의 `FRONTEND_URL` | 배포된 프론트 주소 |
| 구글 OAuth 리다이렉트 URI | `https://3.27.226.133.nip.io/login/oauth2/code/google` |

---

## 4. 첫 배포

첫 배포는 이미지가 아직 없으므로 Actions를 한 번 돌려야 합니다.

- `develop`에 푸시하거나
- Actions 탭 → Deploy to EC2 → Run workflow

배포 후 확인:
```bash
# 액추에이터는 외부에 열려 있지 않으므로 서버 안에서 확인합니다
ssh -i key.pem ubuntu@<도메인> 'curl -s http://127.0.0.1:8080/actuator/health'
```
Swagger: `https://<도메인>/swagger-ui.html`

---

## 5. 운영 중 자주 쓰는 명령

```bash
cd ~/13th-Dortfolio-BE

docker compose -f docker-compose.prod.yml ps          # 상태
docker compose -f docker-compose.prod.yml logs -f spring   # 로그
docker compose -f docker-compose.prod.yml restart spring   # 재시작

# DB 접속
docker exec -it dortfolio-postgres psql -U dortfolio -d dortfolio

# DB 백업
docker exec dortfolio-postgres pg_dump -U dortfolio dortfolio > backup_$(date +%F).sql
```

---

## 6. 직무 역량·강점 태그 임베딩 배치 실행

`develop` 배포 워크플로는 Spring과 FastAPI의 기동을 확인한 뒤
`GENERATE_MISSING` 배치를 자동 실행합니다. 따라서 새 직무·핵심 역량 또는 강점 태그 기준 데이터가
추가되어도 현재 모델의 누락 임베딩이 배포 과정에서 생성됩니다.

일반 서버 시작에서는 `JOB_COMPETENCY_EMBEDDING_BATCH_ENABLED`와
`STRENGTH_TAG_EMBEDDING_BATCH_ENABLED`의 기본값이 `false`이므로
임베딩을 자동 생성하지 않습니다. 자동 배포 외 환경이나 장애 복구 상황에서는 아래 명령으로
배치를 직접 실행할 수 있습니다.

지원하는 명령은 다음 두 가지입니다.

| 명령 | 동작 |
|---|---|
| `GENERATE_MISSING` | `INSIGHT_EMBEDDING_MODEL`을 우선 사용하고, 없으면 `GEMINI_EMBEDDING_MODEL`, 둘 다 없으면 기본 모델을 기준으로 누락된 임베딩만 생성한 뒤 전체 완료 상태를 검증합니다. |
| `VERIFY` | 외부 AI API를 호출하지 않고 전체 개수, 생성 개수, 누락 개수와 누락 ID를 검증합니다. |

### 로컬 실행

PostgreSQL과 FastAPI가 실행 중인 상태에서 PowerShell로 실행합니다.
`SPRING_MAIN_WEB_APPLICATION_TYPE=none`을 지정해 배치 프로세스가 웹 서버로 기동하지 않게 합니다.

누락 임베딩 생성:

```powershell
cd spring
$env:JOB_COMPETENCY_EMBEDDING_BATCH_ENABLED = "true"
$env:JOB_COMPETENCY_EMBEDDING_BATCH_COMMAND = "GENERATE_MISSING"
$env:STRENGTH_TAG_EMBEDDING_BATCH_ENABLED = "true"
$env:STRENGTH_TAG_EMBEDDING_BATCH_COMMAND = "GENERATE_MISSING"
$env:SPRING_MAIN_WEB_APPLICATION_TYPE = "none"
.\gradlew.bat bootRun
```

생성 없이 완료 상태만 검증:

```powershell
cd spring
$env:JOB_COMPETENCY_EMBEDDING_BATCH_ENABLED = "true"
$env:JOB_COMPETENCY_EMBEDDING_BATCH_COMMAND = "VERIFY"
$env:STRENGTH_TAG_EMBEDDING_BATCH_ENABLED = "true"
$env:STRENGTH_TAG_EMBEDDING_BATCH_COMMAND = "VERIFY"
$env:SPRING_MAIN_WEB_APPLICATION_TYPE = "none"
.\gradlew.bat bootRun
```

실행 후 현재 PowerShell 세션에 설정이 남지 않도록 정리합니다.

```powershell
Remove-Item Env:JOB_COMPETENCY_EMBEDDING_BATCH_ENABLED -ErrorAction SilentlyContinue
Remove-Item Env:JOB_COMPETENCY_EMBEDDING_BATCH_COMMAND -ErrorAction SilentlyContinue
Remove-Item Env:STRENGTH_TAG_EMBEDDING_BATCH_ENABLED -ErrorAction SilentlyContinue
Remove-Item Env:STRENGTH_TAG_EMBEDDING_BATCH_COMMAND -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_MAIN_WEB_APPLICATION_TYPE -ErrorAction SilentlyContinue
```

### 운영 Docker Compose 실행

운영 DB와 FastAPI 컨테이너가 실행 중인지 먼저 확인합니다.

```bash
docker compose -f docker-compose.prod.yml ps postgres fastapi
```

누락 임베딩 생성:

```bash
docker compose -f docker-compose.prod.yml run --rm \
  -e JOB_COMPETENCY_EMBEDDING_BATCH_ENABLED=true \
  -e JOB_COMPETENCY_EMBEDDING_BATCH_COMMAND=GENERATE_MISSING \
  -e STRENGTH_TAG_EMBEDDING_BATCH_ENABLED=true \
  -e STRENGTH_TAG_EMBEDDING_BATCH_COMMAND=GENERATE_MISSING \
  -e SPRING_MAIN_WEB_APPLICATION_TYPE=none \
  spring
```

생성 없이 완료 상태만 검증:

```bash
docker compose -f docker-compose.prod.yml run --rm \
  -e JOB_COMPETENCY_EMBEDDING_BATCH_ENABLED=true \
  -e JOB_COMPETENCY_EMBEDDING_BATCH_COMMAND=VERIFY \
  -e STRENGTH_TAG_EMBEDDING_BATCH_ENABLED=true \
  -e STRENGTH_TAG_EMBEDDING_BATCH_COMMAND=VERIFY \
  -e SPRING_MAIN_WEB_APPLICATION_TYPE=none \
  spring
```

생성 결과에는 전체 대상, 신규 생성, 기존 데이터 건너뜀, 실패 건수가 기록됩니다.
항목별 실패가 발생하면 해당 `jobCompetencyId` 또는 `strengthTagId`와 원인이 로그에 남고,
나머지 항목은 계속 처리됩니다.
실패 또는 누락이 하나라도 남으면 명령이 실패하므로 원인을 해결한 뒤 같은 명령을 다시 실행합니다.
이미 생성된 현재 모델의 임베딩은 건너뛰므로 재실행해도 중복 저장되지 않습니다.

---

## 아직 안 된 것 / 논의 필요

- **인증서 갱신 확인** — certbot 컨테이너가 12시간마다 갱신을 시도하지만, 실제 갱신은 만료 30일 전에야 일어납니다.
  발급 후 두 달쯤 뒤에 `docker logs dortfolio-certbot`으로 갱신이 정상 동작했는지 한 번 확인하세요.
- **DB 백업 자동화 없음** — 위 `pg_dump`를 cron에 걸거나, 데이터가 중요해지면 RDS 전환을 검토합니다.
- **스키마 변경 시 마이그레이션 파일 필수** — Flyway를 쓰므로 엔티티만 고치면 배포 시 `validate`에서 실패합니다.
  `spring/src/main/resources/db/migration/`에 `V2__xxx.sql` 형태로 파일을 추가해야 합니다.
  이미 적용된 마이그레이션 파일은 절대 수정하지 않습니다(체크섬 불일치로 실패).
- **단일 인스턴스** — 배포 중 짧은 다운타임이 있습니다.
- **OAuth 리다이렉트 URI** — Google 콘솔에 `https://<도메인>/login/oauth2/code/google`를 등록해야 소셜 로그인이 동작합니다.
- **프론트(Vercel) 연동** — Vercel은 HTTPS로만 서비스되므로 백엔드도 HTTPS여야 브라우저가 API 호출을 막지 않습니다.
  프론트 주소가 정해지면 `.env`의 `FRONTEND_URL`과 CORS 허용 목록을 함께 갱신해야 합니다.
