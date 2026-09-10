# Wedding Project 기능 설명

> 브랜치: `feat/wedding-project`  
> 상태: `dev` 미머지 (2026-09-10 기준)

## 1. 개요

신랑·신부가 **결혼식(Wedding Project)** 을 생성·조회·수정·삭제하고, 하객 초대 링크/QR을 관리하는 기능이다.

연관 요구사항: `FR-PRJ-001` ~ `FR-PRJ-007` (초대 QR은 URL+QR 이미지로 MVP 구현)

## 2. 아키텍처

```text
[Next.js]
  /dashboard
  /dashboard/projects/new
  /dashboard/projects/[id]
  /dashboard/projects/[id]/edit
  /dashboard/projects/[id]/share
        │  JWT Bearer
        ▼
[Spring Boot]
  /api/projects
        │
        ▼
[MySQL]
  wedding_project
  invitation      (생성 시 자동)
  invite_link     (생성 시 자동)
```

## 3. Backend

### 3.1 엔티티

| 엔티티 | 역할 |
|--------|------|
| `WeddingProject` | 결혼식 기본 정보 (신랑/신부, 일시, 장소, slug, status) |
| `Invitation` | 청첩장 stub (제목·인사말, `published=false`) — 다음 단계에서 확장 |
| `InviteLink` | 하객 초대 토큰, 활성/비활성 |

### 3.2 API

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/projects` | 내 Project 목록 |
| `POST` | `/api/projects` | Project 생성 (+ Invitation, InviteLink) |
| `GET` | `/api/projects/{id}` | 상세 |
| `PUT` | `/api/projects/{id}` | 수정 |
| `DELETE` | `/api/projects/{id}` | Soft delete |
| `PATCH` | `/api/projects/{id}/invite-link` | `{ "active": true/false }` |

### 3.3 비즈니스 규칙

- **MVP:** 회원당 Project **1개**만 생성 가능
- 소유자(`owner_id`)만 접근 (아니면 403)
- Soft delete 시 `deletedAt` 설정 + status `ARCHIVED` + 초대 링크 비활성
- `slug` 자동 생성: `{groom}-{bride}-{random8}`

### 3.4 주요 클래스

```text
backend/src/main/java/com/memorywedding/
├── domain/entity/WeddingProject.java
├── domain/entity/Invitation.java
├── domain/entity/InviteLink.java
├── project/WeddingProjectService.java
├── project/dto/*
└── controller/WeddingProjectController.java
```

## 4. Frontend

### 4.1 화면

| 경로 | 역할 |
|------|------|
| `/dashboard` | Project 목록 / 생성 CTA |
| `/dashboard/projects/new` | 생성 폼 |
| `/dashboard/projects/[id]` | 상세 + 삭제 |
| `/dashboard/projects/[id]/edit` | 수정 폼 |
| `/dashboard/projects/[id]/share` | 초대 URL 복사 + QR |

### 4.2 Daum 주소 API

- 스크립트: `https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js`
- **API 키 불필요** (우편번호 팝업 서비스)
- 컴포넌트: `src/components/ui/AddressSearchField.tsx`
- 헬퍼: `src/lib/daum-postcode.ts`
- 동작: 「주소 검색」→ 팝업 → 도로명/지번 주소 입력. 건물명이 있고 예식장명이 비어 있으면 예식장명 자동 채움

### 4.3 예식 일시 (타임존)

- Backend `weddingAt`은 **LocalDateTime** (벽시계, 타임존 없음)
- 프론트는 `Date#toISOString()`을 쓰지 않는다 (UTC로 밀려 시각이 어긋남)
- `src/lib/datetime.ts`: API 전송 `YYYY-MM-DDTHH:mm:ss`, 표시는 시:분까지

## 5. 커밋 이력 (`feat/wedding-project`)

1. `feat: add wedding project CRUD and invite sharing`
2. `feat: integrate Daum postcode API for venue address`

## 6. 아직 안 한 것 (다음 단계 후보)

- 모바일 청첩장 `/w/[slug]` 공개 페이지
- 청첩장 편집 UI (Invitation 상세 편집)
- Google Drive 연동
- 실제 QR 라이브러리/다운로드 파일화 (현재 외부 QR 이미지 URL 사용)

## 7. 로컬 확인

```bash
git checkout feat/wedding-project
docker compose up -d --build
# 로그인 → 대시보드 → Project 만들기 → 주소 검색
```
