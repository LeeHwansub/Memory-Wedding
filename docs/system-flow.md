# System Flow Chart

> Memory Wedding — 시스템 흐름도 및 프로세스 설계

## 문서 정보

| 항목 | 내용 |
|------|------|
| 버전 | 1.0.0 |
| 작성일 | 2026-09-10 |
| 상태 | Draft |
| 참조 | `docs/requirements-spec.md`, `docs/ui-ux-design.md`, `docs/erd.md` |

---

## 1. 시스템 아키텍처 개요

```mermaid
flowchart TB
    subgraph Client["Client"]
        FE["Next.js 15\n(Frontend)"]
    end

    subgraph Server["Backend"]
        API["Spring Boot 3\nREST API"]
        SEC["Spring Security\nOAuth 2.0"]
    end

    subgraph Data["Data Layer"]
        DB[("MySQL 8")]
    end

    subgraph External["External Services"]
        GOOGLE["Google OAuth"]
        NAVER["Naver OAuth"]
        DRIVE["Google Drive API"]
        GEMINI["Gemini API"]
        FFMPEG["FFmpeg"]
    end

    FE <-->|REST API| API
    API --> SEC
    SEC --> GOOGLE
    SEC --> NAVER
    API --> DB
    API --> DRIVE
    API --> GEMINI
    API --> FFMPEG
```

---

## 2. 사용자 Role별 접근 흐름

```mermaid
flowchart LR
    subgraph Public
        LAND["랜딩 /"]
        LOGIN["로그인 /login"]
    end

    subgraph USER["신랑·신부 (USER)"]
        DASH["대시보드"]
        PRJ["Project 관리"]
        INV["청첩장 편집"]
        GAL["갤러리"]
        SHARE["공유·QR"]
    end

    subgraph GUEST["하객 (GUEST)"]
        WED["청첩장 /w/slug"]
        UP["업로드"]
        GB["방명록"]
    end

    subgraph ADMIN["관리자 (ADMIN)"]
        ADM["관리자 패널"]
    end

    LAND --> LOGIN
    LOGIN -->|OAuth| DASH
    DASH --> PRJ --> INV
    PRJ --> GAL
    PRJ --> SHARE
    SHARE -->|초대 링크/QR| WED
    WED --> UP
    WED --> GB
    LOGIN -->|ADMIN Role| ADM
```

---

## 3. OAuth 로그인 흐름

```mermaid
sequenceDiagram
    actor User as 신랑·신부
    participant FE as Next.js
    participant API as Spring Boot
    participant OAuth as Google/Naver
    participant DB as MySQL

    User->>FE: 로그인 버튼 클릭
    FE->>API: GET /api/auth/{provider}
    API->>OAuth: OAuth Authorization Redirect
    OAuth->>User: 로그인·동의 화면
    User->>OAuth: 인증 완료
    OAuth->>API: Authorization Code Callback
    API->>OAuth: Access Token 교환
    OAuth-->>API: User Info
    API->>DB: oauth_account 조회/생성
    API->>DB: member 조회/생성
    API-->>FE: JWT / Session 발급
    FE-->>User: 대시보드 리다이렉트
```

---

## 4. Wedding Project 생성 흐름

```mermaid
sequenceDiagram
    actor User as 신랑·신부
    participant FE as Next.js
    participant API as Spring Boot
    participant DB as MySQL
    participant Drive as Google Drive

    User->>FE: Project 생성 폼 작성
    FE->>API: POST /api/projects
    API->>DB: wedding_project INSERT
    API->>DB: invitation INSERT (기본값)
    API->>DB: invite_link INSERT (token 생성)
    API->>Drive: Wedding_{slug}/ 폴더 생성
    Drive-->>API: drive_root_folder_id
    API->>DB: drive_root_folder_id UPDATE
    API-->>FE: Project 생성 완료
    FE-->>User: Project 상세 페이지
```

---

## 5. 하객 사진·영상 업로드 흐름

```mermaid
sequenceDiagram
    actor Guest as 하객
    participant FE as Next.js
    participant API as Spring Boot
    participant DB as MySQL
    participant Drive as Google Drive

    Guest->>FE: QR/링크 접속 (/w/slug)
    FE->>API: GET /api/public/w/{slug}
    API->>DB: wedding_project + invitation 조회
    API-->>FE: 청첩장 데이터

    Guest->>FE: 이름 입력 + 파일 선택
    FE->>API: POST /api/public/w/{slug}/upload (multipart)
    API->>DB: upload_file INSERT (status: UPLOADING)
    API->>Drive: Photos/{guest_name}/ or Videos/{guest_name}/
    Drive-->>API: drive_file_id
    API->>DB: upload_file UPDATE (status: COMPLETED)
    API-->>FE: 업로드 완료
    FE-->>Guest: 완료 메시지 + 방명록 CTA
```

---

## 6. 방명록 작성 흐름

```mermaid
flowchart TD
    A["하객: /w/slug/guestbook 접속"] --> B{"invite_link\n유효?"}
    B -->|No| C["만료 안내 페이지"]
    B -->|Yes| D["방명록 폼 표시"]
    D --> E["이름 + 메시지 입력"]
    E --> F{"유효성 검증"}
    F -->|Fail| G["오류 메시지"]
    G --> D
    F -->|Pass| H["POST /api/public/w/slug/guestbook"]
    H --> I["guestbook_entry INSERT"]
    I --> J["목록에 즉시 반영"]
```

---

## 7. 청첩장 편집·공개 흐름

```mermaid
stateDiagram-v2
    [*] --> Draft: Project 생성
    Draft --> Editing: 청첩장 편집 시작
    Editing --> Editing: 섹션 수정·저장
    Editing --> Preview: 미리보기
    Preview --> Editing: 수정 계속
    Preview --> Published: 공개 설정
    Published --> Private: 비공개 전환
    Private --> Published: 재공개
    Published --> [*]: 하객 접근 가능
    Private --> [*]: 하객 접근 차단
```

---

## 8. Google Drive 저장 흐름

```mermaid
flowchart TD
    subgraph ProjectCreate["Project 생성 시"]
        P1["Drive OAuth 토큰 확인"] --> P2["Wedding_{slug}/ 생성"]
        P2 --> P3["Photos/, Videos/, AI/, Archive/ 하위 폴더"]
        P3 --> P4["drive_root_folder_id DB 저장"]
    end

    subgraph Upload["하객 업로드 시"]
        U1["파일 유형 판별"] --> U2{"PHOTO or VIDEO?"}
        U2 -->|PHOTO| U3["Photos/{guest_name}/"]
        U2 -->|VIDEO| U4["Videos/{guest_name}/"]
        U3 --> U5["Drive API Upload"]
        U4 --> U5
        U5 --> U6["drive_file_id → upload_file 저장"]
    end

    subgraph Delete["파일 삭제 시"]
        D1["신랑·신부 삭제 요청"] --> D2["Drive API Delete"]
        D2 --> D3["upload_file Soft Delete"]
    end
```

---

## 9. AI 분석 흐름 (MVP 이후)

```mermaid
flowchart TD
    A["신랑·신부: AI 분석 요청"] --> B["ai_analysis_job 생성\nstatus: PENDING"]
    B --> C["비동기 Worker 시작\nstatus: PROCESSING"]
    C --> D["upload_file 목록 조회\n(PHOTO, COMPLETED)"]
    D --> E{"분석 대상\n있음?"}
    E -->|No| F["status: COMPLETED\n결과 없음 안내"]
    E -->|Yes| G["Gemini API 호출\n(사진별)"]
    G --> H["장면 분류\n(ENTRANCE/SONG/GROUP_PHOTO/RECEPTION/OTHER)"]
    H --> I["Best Shot 선정"]
    I --> J["ai_photo_result INSERT"]
    J --> K{"모든 파일\n처리?"}
    K -->|No| G
    K -->|Yes| L["status: COMPLETED"]
    L --> M["신랑·신부 결과 조회"]
```

---

## 10. AI 하이라이트 영상 생성 흐름 (MVP 이후)

```mermaid
sequenceDiagram
    actor User as 신랑·신부
    participant API as Spring Boot
    participant DB as MySQL
    participant Drive as Google Drive
    participant FF as FFmpeg

    User->>API: POST /api/projects/{id}/ai/video
    API->>DB: ai_video_job INSERT (PENDING)
    API->>DB: Best Shot ai_photo_result 조회
    API->>Drive: Best Shot 원본 다운로드
    API->>FF: 사진 시퀀스 + BGM → MP4 합성
    FF-->>API: 영상 파일
    API->>Drive: Archive/ 폴더 업로드
    Drive-->>API: drive_file_id
    API->>DB: ai_video_job UPDATE (COMPLETED)
    API-->>User: 영상 생성 완료
```

---

## 11. 관리자 모니터링 흐름

```mermaid
flowchart LR
    A["관리자 로그인"] --> B{"Role = ADMIN?"}
    B -->|No| C["403 Forbidden"]
    B -->|Yes| D["관리자 대시보드"]
    D --> E["회원 목록"]
    D --> F["Project 목록"]
    D --> G["AI Job 현황"]
    E --> H["회원 검색·상세"]
    F --> I["Project 상세·비공개·삭제"]
    G --> J["분석/영상 Job 상태"]
```

---

## 12. API 요청 처리 공통 흐름

```mermaid
flowchart TD
    REQ["HTTP Request"] --> CORS{"CORS\n검증"}
    CORS -->|Fail| E403["403"]
    CORS -->|Pass| AUTH{"인증\n필요?"}
    AUTH -->|Public API| HANDLER["Controller"]
    AUTH -->|Required| JWT{"JWT/Session\n유효?"}
    JWT -->|No| E401["401 Unauthorized"]
    JWT -->|Yes| ROLE{"Role\n권한?"}
    ROLE -->|Fail| E403B["403 Forbidden"]
    ROLE -->|Pass| HANDLER
    HANDLER --> SERVICE["Service Layer"]
    SERVICE --> REPO["JPA Repository"]
    REPO --> DB[("MySQL")]
    SERVICE --> EXT["External API\n(Drive/Gemini)"]
    HANDLER --> RES["Response DTO"]
```

---

## 13. 배포 흐름 (목표)

```mermaid
flowchart LR
    subgraph Dev["개발"]
        FE_DEV["Next.js Dev"]
        BE_DEV["Spring Boot Dev"]
        DB_DEV[("MySQL Docker")]
    end

    subgraph CI["CI/CD"]
        GH["GitHub Push"]
        BUILD["Build & Test"]
        DOCKER["Docker Image"]
    end

    subgraph Deploy["배포"]
        DEV_ENV["dev 브랜치\n→ Staging"]
        MAIN_ENV["main 브랜치\n→ Production"]
    end

    FE_DEV --> GH
    BE_DEV --> GH
    GH --> BUILD --> DOCKER
    DOCKER --> DEV_ENV
    DEV_ENV -->|검증 완료| MAIN_ENV
```

---

## 14. MVP vs 확장 Flow 범위

| Flow | MVP | Phase 2 |
|------|-----|---------|
| OAuth 로그인 | ✅ | |
| Project CRUD | ✅ | |
| 청첩장 편집·공개 | ✅ | |
| 하객 업로드 | ✅ | |
| Drive 저장 | ✅ | |
| 방명록 | ✅ | |
| 관리자 기본 | ✅ | |
| AI 장면 분류 | | ✅ |
| Best Shot | | ✅ |
| FFmpeg 영상 | | ✅ |
| CI/CD 배포 | | ✅ |

---

## 15. FR 매핑

| Flow | 연관 FR |
|------|---------|
| §3 OAuth 로그인 | FR-MEM-001, 002 |
| §4 Project 생성 | FR-PRJ-001, FR-STR-002 |
| §5 하객 업로드 | FR-GST-002~006, FR-STR-001 |
| §6 방명록 | FR-GBK-001, 002 |
| §7 청첩장 공개 | FR-INV-003, 004 |
| §8 Drive 저장 | FR-STR-001~004 |
| §9 AI 분석 | FR-AI-001~004 |
| §10 AI 영상 | FR-AI-005, 006 |
| §11 관리자 | FR-ADM-001~006 |

---

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|------|------|-----------|--------|
| 1.0.0 | 2026-09-10 | 초안 작성 | - |
