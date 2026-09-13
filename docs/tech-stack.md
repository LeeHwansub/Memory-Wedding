# 기술 스택 확정

> Memory Wedding — 최종 기술 스택 및 버전

## 문서 정보

| 항목 | 내용 |
|------|------|
| 버전 | 1.0.0 |
| 작성일 | 2026-09-10 |
| 상태 | Confirmed |

---

## 프로젝트 구조

```text
Memory-Wedding/
├── src/                  # Next.js Frontend
├── backend/              # Spring Boot Backend
├── docs/                 # 설계 문서
├── docker-compose.yml    # MySQL (로컬 개발)
└── README.md
```

---

## Frontend

| 기술 | 버전 | 용도 |
|------|------|------|
| Next.js | 15.5.x | App Router, SSR/CSR |
| React | 19.1.x | UI 컴포넌트 |
| TypeScript | 5.9.x | 정적 타입 |
| Tailwind CSS | 4.1.x | 스타일링 |

**경로:** 프로젝트 루트 (`/`)

---

## Backend

| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 21 LTS | Backend 언어 |
| Spring Boot | 3.4.x | REST API |
| Spring Data JPA | 3.4.x | ORM |
| Spring Security | 6.x | 인증·인가 |
| Gradle | 8.x | 빌드 |

**경로:** `backend/`

---

## Database

| 기술 | 버전 | 용도 |
|------|------|------|
| MySQL | 8.4 | 관계형 DB |
| Docker Compose | - | 로컬 MySQL |

---

## External Services

| 서비스 | 버전/Spec | 용도 | MVP |
|--------|-----------|------|-----|
| Google OAuth 2.0 | OAuth 2.0 | 로그인 | ✅ |
| Naver OAuth 2.0 | OAuth 2.0 | 로그인 | ✅ |
| Kakao OAuth 2.0 | OAuth 2.0 | 로그인 | ✅ |
| Google Drive API | v3 | 파일 저장 | ✅ |
| Gemini API | 2.5 Flash | 장면 분류 · Best Shot | ✅ 1차 (`docs/ai-analysis.md`) |
| FFmpeg | (backend 이미지) | 프레임 추출 · 하이라이트 합성 | ✅ 1차 |

---

## DevOps

| 기술 | 용도 |
|------|------|
| Git / GitHub | 버전 관리 |
| Docker Compose | MySQL + Backend + Frontend 통합 관리 |
| GitHub Actions | CI/CD (추후) |

```bash
docker compose up -d --build   # 전체 스택 실행
```

---

## 브랜치 전략

| 브랜치 | 용도 |
|--------|------|
| `main` | Production |
| `dev` | Development 통합 |
| `feat/*` | 기능별 작업 |

---

## 로컬 개발 포트

| 서비스 | 포트 |
|--------|------|
| Next.js | 3000 |
| Spring Boot | 8080 |
| MySQL | 3307 (호스트) / 3306 (컨테이너) |

---

## 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| 1.0.0 | 2026-09-10 | 초안 확정 |
