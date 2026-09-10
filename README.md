# Memory Wedding

결혼식의 소중한 순간을 하객과 함께 모으고, 오래도록 간직하는 디지털 웨딩 앨범입니다.

## 프로젝트 구조

```text
Memory-Wedding/
├── src/                  # Next.js Frontend
├── backend/              # Spring Boot Backend
├── docs/                 # 설계 문서
├── docker-compose.yml    # MySQL (로컬)
└── README.md
```

## 기술 스택

| 영역 | 기술 |
|------|------|
| Frontend | Next.js 15, React 19, TypeScript, Tailwind CSS 4 |
| Backend | Java 21, Spring Boot 3.4, Spring Data JPA, Spring Security |
| Database | MySQL 8.4 |
| Storage | Google Drive API (예정) |

상세: [`docs/tech-stack.md`](docs/tech-stack.md)

## 시작하기

### 1. 환경 변수

```bash
cp .env.example .env.local
```

### 2. MySQL (Docker)

```bash
docker compose up -d
```

### 3. Frontend

```bash
npm install
npm run dev
```

→ [http://localhost:3000](http://localhost:3000)

### 4. Backend

```bash
cd backend
./gradlew bootRun
```

→ [http://localhost:8080/api/health](http://localhost:8080/api/health)

## 설계 문서

| 문서 | 설명 |
|------|------|
| [`docs/requirements-spec.md`](docs/requirements-spec.md) | 기능 요구사항 |
| [`docs/ui-ux-design.md`](docs/ui-ux-design.md) | UI/UX 설계 |
| [`docs/erd.md`](docs/erd.md) | ERD |
| [`docs/system-flow.md`](docs/system-flow.md) | 시스템 흐름도 |
| [`docs/tech-stack.md`](docs/tech-stack.md) | 기술 스택 |

## 브랜치 전략

| 브랜치 | 용도 |
|--------|------|
| `main` | Production |
| `dev` | Development 통합 |
| `feat/*` | 기능별 작업 |

## 라이선스

Private
