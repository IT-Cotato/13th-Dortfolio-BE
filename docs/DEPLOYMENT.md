# 배포 가이드 (EC2 + GitHub Actions)

`develop`에 푸시하면 GitHub Actions가 테스트 → 이미지 빌드 → GHCR 푸시 → EC2 배포까지 자동으로 진행합니다.

```
GitHub(develop) → Actions(테스트/빌드) → GHCR(이미지) → EC2(docker compose)
                                                          ├─ spring
                                                          ├─ postgres (pgvector)
                                                          ├─ redis
                                                          └─ fastapi
```

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
| HTTP | 80 | 0.0.0.0/0 | API |

DB(5432)와 Redis(6379)는 **절대 열지 않습니다.** 컨테이너 네트워크 안에서만 통신하며,
외부에서 DB를 봐야 하면 SSH 터널을 씁니다.

```bash
ssh -i key.pem -L 5432:localhost:5432 ubuntu@<EC2_IP>
```

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

## 4. 첫 배포

첫 배포는 이미지가 아직 없으므로 Actions를 한 번 돌려야 합니다.

- `develop`에 푸시하거나
- Actions 탭 → Deploy to EC2 → Run workflow

배포 후 확인:
```bash
curl http://<EC2_IP>/actuator/health      # {"status":"UP"}
```
Swagger: `http://<EC2_IP>/swagger-ui.html`

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

## 아직 안 된 것 / 논의 필요

- **HTTPS 없음** — 지금은 HTTP만 열려 있습니다. 프론트가 HTTPS로 배포되면 브라우저가 HTTP API 호출을 막으므로,
  도메인을 준비해 Nginx + Let's Encrypt를 붙이거나 ALB를 두어야 합니다.
- **DB 백업 자동화 없음** — 위 `pg_dump`를 cron에 걸거나, 데이터가 중요해지면 RDS 전환을 검토합니다.
- **스키마 변경 시 마이그레이션 파일 필수** — Flyway를 쓰므로 엔티티만 고치면 배포 시 `validate`에서 실패합니다.
  `spring/src/main/resources/db/migration/`에 `V2__xxx.sql` 형태로 파일을 추가해야 합니다.
  이미 적용된 마이그레이션 파일은 절대 수정하지 않습니다(체크섬 불일치로 실패).
- **단일 인스턴스** — 배포 중 짧은 다운타임이 있습니다.
- **OAuth 리다이렉트 URI** — Google 콘솔에 `http://<EC2_IP>/login/oauth2/code/google`를 등록해야 소셜 로그인이 동작합니다.
