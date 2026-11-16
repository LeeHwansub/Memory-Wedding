# Memory Wedding

결혼식의 소중한 순간을 하객과 함께 모으고, 오래도록 간직하는 디지털 웨딩 앨범입니다.

## 프로젝트 구조

```text
Memory-Wedding/
├── src/                  # Next.js Frontend
├── backend/              # Spring Boot Backend
├── docs/                 # 설계 문서
├── docker-compose.yml    # 전체 스택 (MySQL + Backend + Frontend)
└── README.md
```

## Docker로 전체 실행 (권장)

```bash
# 1. 환경 변수 설정
cp .env.example .env
# .env 파일에 OAuth Client ID/Secret 입력

# 2. 전체 스택 실행
docker compose up -d --build

# 3. 확인
# Frontend: http://localhost:3000
# Backend:  http://localhost:8080/api/health
# MySQL:    localhost:3307
```

### OAuth 설정

Google / Naver / Kakao Developer Console에서 Redirect URI 등록:

| Provider | Redirect URI |
|----------|--------------|
| Google | `http://localhost:8080/login/oauth2/code/google` |
| Naver | `http://localhost:8080/login/oauth2/code/naver` |
| Kakao | `http://localhost:8080/login/oauth2/code/kakao` |

Kakao 동의항목은 **닉네임만** 사용합니다. (이메일 미요청, `kakao_{id}@kakao.local`로 저장)

### 카카오맵 (오시는 길)

1. [Kakao Developers](https://developers.kakao.com) → 내 애플리케이션 → **앱 키 → JavaScript 키** 복사
2. `.env`에 `NEXT_PUBLIC_KAKAO_MAP_APP_KEY=` 로 설정
3. 앱 설정 → 플랫폼 → Web에 `http://localhost:3000` 등록
4. (선택) 카카오맵 API 사용 설정 활성화

`.env` 파일에 Client ID/Secret 입력 후 `docker compose up -d --build` 재실행.

### Docker 명령어

```bash
docker compose up -d          # 백그라운드 실행
docker compose logs -f        # 로그 확인
docker compose down           # 중지
docker compose down -v        # 중지 + DB 데이터 삭제
```

## 로컬 개발 (Docker 없이)

### MySQL

```bash
docker compose up -d mysql
```

### Frontend

```bash
npm install
cp .env.example .env.local
npm run dev
```

### Backend

```bash
cd backend
./gradlew bootRun
```

## 기술 스택

| 영역 | 기술 |
|------|------|
| Frontend | Next.js 15, React 19, TypeScript, Tailwind CSS 4 |
| Backend | Java 21, Spring Boot 3.4, Spring Security OAuth2, JWT |
| Database | MySQL 8.4 |
| Infra | Docker Compose |

상세: [`docs/tech-stack.md`](docs/tech-stack.md)

## API (회원)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/oauth2/authorization/{google\|naver\|kakao}` | OAuth 로그인 |
| GET | `/api/members/me` | 내 프로필 조회 |
| PATCH | `/api/members/me` | 프로필 수정 |
| DELETE | `/api/members/me` | 회원 탈퇴 |

## 설계 문서

| 문서 | 설명 |
|------|------|
| [`docs/requirements-spec.md`](docs/requirements-spec.md) | 기능 요구사항 |
| [`docs/ui-ux-design.md`](docs/ui-ux-design.md) | UI/UX 설계 |
| [`docs/erd.md`](docs/erd.md) | ERD |
| [`docs/system-flow.md`](docs/system-flow.md) | 시스템 흐름도 |

## 브랜치 전략

| 브랜치 | 용도 |
|--------|------|
| `main` | Production |
| `dev` | Development 통합 |
| `feat/*` | 기능별 작업 |

## 라이선스

Private
