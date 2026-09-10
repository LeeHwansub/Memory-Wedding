# 화면 구성 및 UI/UX 설계

> Memory Wedding — 모바일 청첩장 + 하객 참여형 사진·영상 아카이빙 플랫폼

## 문서 정보

| 항목 | 내용 |
|------|------|
| 버전 | 1.0.0 |
| 작성일 | 2026-09-10 |
| 상태 | Draft |
| 참조 | `docs/requirements-spec.md` |

---

## 1. 설계 원칙

| 원칙 | 설명 |
|------|------|
| Mobile First | 청첩장·하객 페이지는 모바일(375px) 기준 설계, 데스크톱은 확장 |
| 3-Tap Rule | 하객은 QR 스캔 후 3탭 이내 업로드·방명록 작성 완료 |
| Role 분리 | USER(신랑·신부), GUEST(하객), ADMIN UI 완전 분리 |
| 감성 UI | 웨딩 톤(따뜻한 베이지·골드·화이트), 과도한 장식 지양 |
| 접근성 | 최소 터치 영역 44px, 고대비 텍스트, 업로드 진행률 명확 표시 |

### 컬러 팔레트

| 용도 | 색상 | Tailwind |
|------|------|----------|
| Background | `#FAF8F5` | `bg-[#FAF8F5]` |
| Foreground | `#2C2420` | `text-[#2C2420]` |
| Accent | `#C9A87C` | `text-[#C9A87C]` |
| Accent Soft | `#F3EBE0` | `bg-[#F3EBE0]` |
| Muted | `#8A7F78` | `text-[#8A7F78]` |
| Error | `#DC2626` | `text-red-600` |
| Success | `#16A34A` | `text-green-600` |

### 타이포그래피

| 용도 | 폰트 | 크기 (mobile) |
|------|------|---------------|
| Display | Playfair Display | 32–40px |
| Heading | Geist Sans Medium | 20–24px |
| Body | Geist Sans Regular | 14–16px |
| Caption | Geist Sans Regular | 12px |

---

## 2. 사이트맵

```text
Memory Wedding
│
├── / (Landing)                          [Public]
│
├── /login                                 [Public]
│
├── /dashboard                             [USER]
│   ├── /dashboard/projects                [USER] Project 목록
│   ├── /dashboard/projects/new            [USER] Project 생성
│   └── /dashboard/projects/[id]           [USER] Project 상세·관리
│       ├── /dashboard/projects/[id]/edit          [USER] Project 수정
│       ├── /dashboard/projects/[id]/invitation    [USER] 청첩장 편집
│       ├── /dashboard/projects/[id]/gallery       [USER] 업로드 갤러리
│       ├── /dashboard/projects/[id]/guestbook     [USER] 방명록 관리
│       ├── /dashboard/projects/[id]/share         [USER] 초대 링크·QR
│       └── /dashboard/projects/[id]/ai            [USER] AI 결과 (MVP 이후)
│
├── /settings                              [USER] 프로필·계정 설정
│
├── /w/[slug]                              [GUEST] 모바일 청첩장 (공개)
│   ├── /w/[slug]/upload                   [GUEST] 사진·영상 업로드
│   └── /w/[slug]/guestbook                [GUEST] 방명록 작성·조회
│
└── /admin                                 [ADMIN]
    ├── /admin/members                     [ADMIN] 회원 관리
    ├── /admin/projects                    [ADMIN] Project 관리
    └── /admin/ai-jobs                     [ADMIN] AI 처리 현황 (MVP 이후)
```

---

## 3. 화면 목록

### 3.1 공통 (Public)

| ID | 화면명 | 경로 | 설명 | 연관 FR |
|----|--------|------|------|---------|
| UI-PUB-001 | 랜딩 | `/` | 서비스 소개, 로그인 CTA | - |
| UI-PUB-002 | 로그인 | `/login` | Google / Naver OAuth 버튼 | FR-MEM-001, 002 |

### 3.2 신랑·신부 (USER)

| ID | 화면명 | 경로 | 설명 | 연관 FR |
|----|--------|------|------|---------|
| UI-USR-001 | 대시보드 | `/dashboard` | Project 목록, 신규 생성 CTA | FR-PRJ-002 |
| UI-USR-002 | Project 생성 | `/dashboard/projects/new` | 예식 정보 입력 폼 | FR-PRJ-001 |
| UI-USR-003 | Project 상세 | `/dashboard/projects/[id]` | 탭: 청첩장·갤러리·방명록·공유·설정 | FR-PRJ-003 |
| UI-USR-004 | Project 수정 | `/dashboard/projects/[id]/edit` | 기본 정보 수정 | FR-PRJ-004 |
| UI-USR-005 | 청첩장 편집 | `/dashboard/projects/[id]/invitation` | 섹션별 WYSIWYG 편집 | FR-INV-003, 004, 005 |
| UI-USR-006 | 갤러리 | `/dashboard/projects/[id]/gallery` | 업로드 파일 그리드·삭제 | FR-STR-003, 004 |
| UI-USR-007 | 방명록 관리 | `/dashboard/projects/[id]/guestbook` | 방명록 목록·삭제 | FR-GBK-002, 003 |
| UI-USR-008 | 공유 | `/dashboard/projects/[id]/share` | 초대 URL, QR 다운로드 | FR-PRJ-006, 007 |
| UI-USR-009 | AI 결과 | `/dashboard/projects/[id]/ai` | 장면 분류·Best Shot (MVP 이후) | FR-AI-003, 004 |
| UI-USR-010 | 설정 | `/settings` | 프로필, 로그아웃, 탈퇴 | FR-MEM-003~006 |

### 3.3 하객 (GUEST)

| ID | 화면명 | 경로 | 설명 | 연관 FR |
|----|--------|------|------|---------|
| UI-GST-001 | 모바일 청첩장 | `/w/[slug]` | 인사말, 예식 정보, CTA(업로드·방명록) | FR-INV-002, FR-GST-001 |
| UI-GST-002 | 사진·영상 업로드 | `/w/[slug]/upload` | 다중 파일 선택, 이름 입력, 진행률 | FR-GST-002~006 |
| UI-GST-003 | 방명록 | `/w/[slug]/guestbook` | 작성 폼 + 목록 | FR-GBK-001, 002 |
| UI-GST-004 | 링크 만료 안내 | `/w/[slug]/expired` | 유효하지 않은 초대 링크 | FR-GST-001 |

### 3.4 관리자 (ADMIN)

| ID | 화면명 | 경로 | 설명 | 연관 FR |
|----|--------|------|------|---------|
| UI-ADM-001 | 관리자 로그인 | `/admin/login` | 관리자 인증 | FR-ADM-001 |
| UI-ADM-002 | 관리자 대시보드 | `/admin` | 요약 통계 (MVP 이후) | FR-ADM-005 |
| UI-ADM-003 | 회원 관리 | `/admin/members` | 회원 목록·검색 | FR-ADM-002 |
| UI-ADM-004 | Project 관리 | `/admin/projects` | 전체 Project 목록·상세 | FR-ADM-003, 004 |

---

## 4. 핵심 사용자 플로우

### 4.1 신랑·신부 — Wedding Project 생성 ~ 공유

```text
랜딩 → 로그인(OAuth) → 대시보드
  → [Project 만들기]
  → 예식 정보 입력 (신랑·신부 이름, 일시, 장소)
  → Project 상세
  → [청첩장 편집] → 섹션 편집 → [미리보기] → [공개]
  → [공유] → URL 복사 / QR 다운로드
```

### 4.2 하객 — QR 접속 ~ 사진 업로드

```text
QR 스캔 → 모바일 청첩장 (/w/[slug])
  → [사진 올리기] 탭
  → 이름 입력 → 파일 선택 (다중)
  → 업로드 진행률 표시
  → 완료 메시지 + [방명록 남기기] CTA
```

### 4.3 하객 — 방명록 작성

```text
모바일 청첩장 → [축하 메시지] 탭
  → 이름 + 메시지 입력 → [남기기]
  → 방명록 목록에 즉시 반영
```

### 4.4 신랑·신부 — 업로드 확인

```text
대시보드 → Project 상세 → [갤러리] 탭
  → 그리드 뷰 (날짜·하객명 필터)
  → 사진 클릭 → 상세 / 삭제
```

---

## 5. 화면별 와이어프레임 (Mobile 375px)

### UI-PUB-001 — 랜딩

```text
┌─────────────────────────┐
│      Memory Wedding     │
│                         │
│   우리의 결혼식,         │
│   모든 순간을 함께       │
│                         │
│  [  시작하기 (로그인)  ] │
│                         │
│  ┌─────┐ ┌─────┐ ┌─────┐│
│  │청첩장│ │업로드│ │방명록││
│  └─────┘ └─────┘ └─────┘│
└─────────────────────────┘
```

### UI-GST-001 — 모바일 청첩장 (하객)

```text
┌─────────────────────────┐
│  ♡ 신랑 ♥ 신부 ♡         │
│                         │
│  [  웨딩 사진 Hero  ]   │
│                         │
│  2026. 00. 00 (토) 14:00│
│  ○○ 웨딩홀 3F 그랜드홀   │
│                         │
│  ── 인사말 ──            │
│  소중한 분들을 초대...   │
│                         │
│ ┌──────────┐┌──────────┐│
│ │📷 사진   ││💌 방명록  ││
│ │  올리기  ││  남기기  ││
│ └──────────┘└──────────┘│
│                         │
│  ── 오시는 길 ──         │
│  [ 지도 ]               │
└─────────────────────────┘
```

### UI-GST-002 — 사진·영상 업로드

```text
┌─────────────────────────┐
│  ←  사진·영상 올리기     │
│                         │
│  이름                    │
│  ┌───────────────────┐  │
│  │ 홍길동             │  │
│  └───────────────────┘  │
│                         │
│  ┌───────────────────┐  │
│  │                   │  │
│  │   📁 파일 선택     │  │
│  │   (다중 선택 가능)  │  │
│  │                   │  │
│  └───────────────────┘  │
│                         │
│  선택된 파일 (3)         │
│  ┌──┐ ┌──┐ ┌──┐        │
│  │  │ │  │ │  │        │
│  └──┘ └──┘ └──┘        │
│                         │
│  ████████░░  80%        │
│                         │
│  [      업로드하기      ]│
└─────────────────────────┘
```

### UI-USR-003 — Project 상세 (신랑·신부)

```text
┌─────────────────────────┐
│  ←  김○○ ♥ 이○○          │
│                         │
│  ┌─────┬─────┬─────┬───┐│
│  │청첩장│갤러리│방명록│공유││
│  └─────┴─────┴─────┴───┘│
│                         │
│  (선택된 탭 콘텐츠)       │
│                         │
│  [ 청첩장 편집 ]          │
│  [ 미리보기 ]            │
│  공개 ●────○ 비공개      │
│                         │
└─────────────────────────┘
```

### UI-USR-008 — 공유 (초대 링크·QR)

```text
┌─────────────────────────┐
│  ←  하객 초대            │
│                         │
│  초대 링크               │
│  ┌───────────────────┐  │
│  │ memory.wedding/w/…│📋│
│  └───────────────────┘  │
│                         │
│  ┌───────────────────┐  │
│  │                   │  │
│  │    [ QR CODE ]    │  │
│  │                   │  │
│  └───────────────────┘  │
│                         │
│  [ QR 이미지 저장 ]       │
│  [ 링크 비활성화 ]        │
└─────────────────────────┘
```

---

## 6. 컴포넌트 설계

### 공통 컴포넌트

| 컴포넌트 | 설명 | 사용 화면 |
|----------|------|-----------|
| `Button` | Primary / Secondary / Ghost | 전체 |
| `Input` | Text, Textarea | 폼 전체 |
| `FileUploader` | Drag & drop + 다중 선택 + 진행률 | GST-002 |
| `QRCodeDisplay` | QR 생성 및 다운로드 | USR-008 |
| `OAuthButton` | Google / Naver 로그인 | PUB-002 |
| `TabNav` | Project 상세 탭 | USR-003 |
| `PhotoGrid` | Masonry / Grid 갤러리 | USR-006, GST |
| `GuestbookCard` | 방명록 카드 | GST-003, USR-007 |
| `EmptyState` | 데이터 없음 안내 | 전체 |
| `Toast` | 성공/오류 알림 | 전체 |
| `ConfirmDialog` | 삭제·탈퇴 확인 | USR, ADM |

### 레이아웃

| 레이아웃 | 설명 |
|----------|------|
| `PublicLayout` | 랜딩, 로그인 |
| `DashboardLayout` | USER — 하단/사이드 네비 |
| `GuestLayout` | GUEST — 최소 UI, 청첩장 브랜딩 |
| `AdminLayout` | ADMIN — 테이블 중심 데스크톱 |

---

## 7. 반응형 브레이크포인트

| Breakpoint | Width | 적용 |
|------------|-------|------|
| Mobile | < 640px | 기본 (청첩장, 하객) |
| Tablet | 640–1024px | 대시보드 2열 |
| Desktop | > 1024px | 관리자, 대시보드 사이드바 |

---

## 8. MVP 화면 우선순위

### Phase 1 — MVP

| 우선순위 | 화면 ID | 화면명 |
|----------|---------|--------|
| P0 | UI-PUB-001, 002 | 랜딩, 로그인 |
| P0 | UI-USR-001~008 | 대시보드, Project CRUD, 청첩장, 갤러리, 방명록, 공유 |
| P0 | UI-GST-001~003 | 청첩장, 업로드, 방명록 |
| P1 | UI-ADM-001, 003, 004 | 관리자 기본 |
| P1 | UI-USR-010 | 설정 |

### Phase 2 — AI 확장

| 우선순위 | 화면 ID | 화면명 |
|----------|---------|--------|
| P2 | UI-USR-009 | AI 결과 |
| P2 | UI-ADM-002, AI jobs | 관리자 AI 모니터링 |

---

## 9. UX 상태 정의

| 상태 | 처리 |
|------|------|
| Loading | Skeleton UI (갤러리, 목록) |
| Empty | EmptyState + CTA (예: "첫 Project 만들기") |
| Error | Toast + 재시도 버튼 |
| Uploading | Progress bar + 취소 (선택) |
| Offline | "네트워크 연결을 확인해주세요" (MVP 이후) |

---

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|------|------|-----------|--------|
| 1.0.0 | 2026-09-10 | 초안 작성 | - |
