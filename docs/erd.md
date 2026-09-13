# ERD (Entity Relationship Diagram)

> Memory Wedding — MySQL 8.x / Spring Data JPA

## 문서 정보

| 항목 | 내용 |
|------|------|
| 버전 | 1.0.0 |
| 작성일 | 2026-09-10 |
| 상태 | Draft |
| 참조 | `docs/requirements-spec.md`, `docs/ui-ux-design.md` |

---

## 1. ERD 다이어그램

```mermaid
erDiagram
    MEMBER ||--o{ OAUTH_ACCOUNT : has
    MEMBER ||--o{ WEDDING_PROJECT : owns
    MEMBER ||--o| DRIVE_CONNECTION : connects

    WEDDING_PROJECT ||--|| INVITATION : has
    WEDDING_PROJECT ||--|| INVITE_LINK : has
    WEDDING_PROJECT ||--o{ UPLOAD_FILE : contains
    WEDDING_PROJECT ||--o{ GUESTBOOK_ENTRY : contains
    WEDDING_PROJECT ||--o{ AI_ANALYSIS_JOB : triggers
    WEDDING_PROJECT ||--o{ AI_VIDEO_JOB : triggers

    UPLOAD_FILE ||--o| AI_PHOTO_RESULT : analyzed_by
    AI_ANALYSIS_JOB ||--o{ AI_PHOTO_RESULT : produces

    MEMBER {
        bigint id PK
        varchar email UK
        varchar display_name
        enum role
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    OAUTH_ACCOUNT {
        bigint id PK
        bigint member_id FK
        enum provider
        varchar provider_user_id
        datetime created_at
    }

    WEDDING_PROJECT {
        bigint id PK
        bigint owner_id FK
        varchar slug UK
        varchar groom_name
        varchar bride_name
        datetime wedding_at
        varchar venue_name
        varchar venue_address
        enum status
        varchar drive_root_folder_id
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    INVITATION {
        bigint id PK
        bigint project_id FK UK
        varchar title
        text greeting_message
        boolean is_published
        json sections
        varchar map_url
        json account_info
        datetime created_at
        datetime updated_at
    }

    INVITE_LINK {
        bigint id PK
        bigint project_id FK UK
        varchar token UK
        boolean is_active
        datetime expires_at
        datetime created_at
    }

    UPLOAD_FILE {
        bigint id PK
        bigint project_id FK
        enum file_type
        varchar guest_name
        varchar original_filename
        varchar mime_type
        bigint file_size
        varchar drive_file_id
        varchar drive_folder_path
        enum upload_status
        datetime created_at
        datetime deleted_at
    }

    GUESTBOOK_ENTRY {
        bigint id PK
        bigint project_id FK
        varchar guest_name
        text message
        datetime created_at
        datetime deleted_at
    }

    DRIVE_CONNECTION {
        bigint id PK
        bigint member_id FK UK
        text refresh_token
        varchar drive_root_folder_id
        datetime token_expires_at
        datetime created_at
        datetime updated_at
    }

    AI_ANALYSIS_JOB {
        bigint id PK
        bigint project_id FK
        bigint requested_by FK
        enum status
        int total_files
        int processed_files
        datetime started_at
        datetime completed_at
        datetime created_at
    }

    AI_PHOTO_RESULT {
        bigint id PK
        bigint job_id FK
        bigint upload_file_id FK UK
        enum scene_category
        boolean is_best_shot
        decimal confidence
        json metadata
        datetime analyzed_at
    }

    AI_VIDEO_JOB {
        bigint id PK
        bigint project_id FK
        bigint requested_by FK
        enum status
        varchar drive_file_id
        varchar error_message
        datetime started_at
        datetime completed_at
        datetime created_at
    }
```

---

## 2. 엔티티 목록

| # | 테이블명 | 설명 | MVP |
|---|----------|------|-----|
| 1 | `member` | 신랑·신부·관리자 회원 | ✅ |
| 2 | `oauth_account` | OAuth 제공자 연동 정보 | ✅ |
| 3 | `wedding_project` | 결혼식(Project) 단위 | ✅ |
| 4 | `invitation` | 모바일 청첩장 | ✅ |
| 5 | `invite_link` | 하객 초대 링크·QR | ✅ |
| 6 | `upload_file` | 업로드 파일 Metadata | ✅ |
| 7 | `guestbook_entry` | 방명록 | ✅ |
| 8 | `drive_connection` | Google Drive OAuth 연동 | ✅ |
| 9 | `ai_analysis_job` | AI 사진 분석 작업 | ✅ (1차) |
| 10 | `ai_photo_result` | AI 장면 분류·Best Shot 결과 | ✅ (1차) |
| 11 | `ai_video_job` | FFmpeg 하이라이트 + Drive AI/Archive | ✅ (1차) |

---

## 3. 테이블 상세

### 3.1 member

| 컬럼 | 타입 | Null | Key | 설명 |
|------|------|------|-----|------|
| id | BIGINT | NO | PK | Auto Increment |
| email | VARCHAR(255) | NO | UK | OAuth 이메일 |
| display_name | VARCHAR(100) | NO | | 표시 이름 |
| role | ENUM('USER','ADMIN') | NO | | 권한 (기본 USER) |
| created_at | DATETIME | NO | | 생성일 |
| updated_at | DATETIME | NO | | 수정일 |
| deleted_at | DATETIME | YES | | Soft Delete |

**인덱스:** `idx_member_email`, `idx_member_deleted_at`

---

### 3.2 oauth_account

| 컬럼 | 타입 | Null | Key | 설명 |
|------|------|------|-----|------|
| id | BIGINT | NO | PK | Auto Increment |
| member_id | BIGINT | NO | FK → member.id | 회원 |
| provider | ENUM('GOOGLE','NAVER','KAKAO') | NO | | OAuth 제공자 |
| provider_user_id | VARCHAR(255) | NO | | 제공자 사용자 ID |
| created_at | DATETIME | NO | | 생성일 |

**UK:** `(provider, provider_user_id)`  
**FK:** `member_id` → `member(id)` ON DELETE CASCADE

---

### 3.3 wedding_project

| 컬럼 | 타입 | Null | Key | 설명 |
|------|------|------|-----|------|
| id | BIGINT | NO | PK | Auto Increment |
| owner_id | BIGINT | NO | FK → member.id | 소유자 (신랑·신부) |
| slug | VARCHAR(50) | NO | UK | 하객 URL 식별자 (`/w/[slug]`) |
| groom_name | VARCHAR(50) | NO | | 신랑 이름 |
| bride_name | VARCHAR(50) | NO | | 신부 이름 |
| wedding_at | DATETIME | NO | | 예식 일시 |
| venue_name | VARCHAR(200) | YES | | 예식장명 |
| venue_address | VARCHAR(500) | YES | | 예식장 주소 |
| status | ENUM('DRAFT','ACTIVE','ARCHIVED') | NO | | Project 상태 |
| drive_root_folder_id | VARCHAR(100) | YES | | Drive 루트 폴더 ID |
| created_at | DATETIME | NO | | 생성일 |
| updated_at | DATETIME | NO | | 수정일 |
| deleted_at | DATETIME | YES | | Soft Delete |

**FK:** `owner_id` → `member(id)`  
**인덱스:** `idx_project_owner_id`, `idx_project_slug`

---

### 3.4 invitation

| 컬럼 | 타입 | Null | Key | 설명 |
|------|------|------|-----|------|
| id | BIGINT | NO | PK | Auto Increment |
| project_id | BIGINT | NO | FK, UK | Wedding Project (1:1) |
| title | VARCHAR(200) | YES | | 청첩장 제목 |
| greeting_message | TEXT | YES | | 인사말 |
| is_published | BOOLEAN | NO | | 공개 여부 (기본 false) |
| sections | JSON | YES | | 섹션별 콘텐츠 (갤러리, 계좌 등) |
| map_url | VARCHAR(500) | YES | | 지도 URL |
| account_info | JSON | YES | | 축의금 계좌 정보 |
| created_at | DATETIME | NO | | 생성일 |
| updated_at | DATETIME | NO | | 수정일 |

**FK:** `project_id` → `wedding_project(id)` ON DELETE CASCADE

---

### 3.5 invite_link

| 컬럼 | 타입 | Null | Key | 설명 |
|------|------|------|-----|------|
| id | BIGINT | NO | PK | Auto Increment |
| project_id | BIGINT | NO | FK, UK | Wedding Project (1:1) |
| token | VARCHAR(64) | NO | UK | 초대 토큰 |
| is_active | BOOLEAN | NO | | 활성 여부 (기본 true) |
| expires_at | DATETIME | YES | | 만료일 (null = 무기한) |
| created_at | DATETIME | NO | | 생성일 |

**FK:** `project_id` → `wedding_project(id)` ON DELETE CASCADE

---

### 3.6 upload_file

| 컬럼 | 타입 | Null | Key | 설명 |
|------|------|------|-----|------|
| id | BIGINT | NO | PK | Auto Increment |
| project_id | BIGINT | NO | FK | Wedding Project |
| file_type | ENUM('PHOTO','VIDEO') | NO | | 파일 유형 |
| guest_name | VARCHAR(50) | NO | | 업로드 하객 이름 |
| original_filename | VARCHAR(255) | NO | | 원본 파일명 |
| mime_type | VARCHAR(100) | NO | | MIME Type |
| file_size | BIGINT | NO | | 파일 크기 (bytes) |
| drive_file_id | VARCHAR(100) | YES | | Google Drive File ID |
| drive_folder_path | VARCHAR(500) | YES | | Drive 폴더 경로 |
| upload_status | ENUM('PENDING','UPLOADING','COMPLETED','FAILED') | NO | | 업로드 상태 |
| created_at | DATETIME | NO | | 생성일 |
| deleted_at | DATETIME | YES | | Soft Delete |

**FK:** `project_id` → `wedding_project(id)` ON DELETE CASCADE  
**인덱스:** `idx_upload_project_id`, `idx_upload_file_type`, `idx_upload_guest_name`

---

### 3.7 guestbook_entry

| 컬럼 | 타입 | Null | Key | 설명 |
|------|------|------|-----|------|
| id | BIGINT | NO | PK | Auto Increment |
| project_id | BIGINT | NO | FK | Wedding Project |
| guest_name | VARCHAR(50) | NO | | 하객 이름 |
| message | TEXT | NO | | 축하 메시지 |
| created_at | DATETIME | NO | | 작성일 |
| deleted_at | DATETIME | YES | | Soft Delete |

**FK:** `project_id` → `wedding_project(id)` ON DELETE CASCADE  
**인덱스:** `idx_guestbook_project_id`, `idx_guestbook_created_at`

---

### 3.8 drive_connection

| 컬럼 | 타입 | Null | Key | 설명 |
|------|------|------|-----|------|
| id | BIGINT | NO | PK | Auto Increment |
| member_id | BIGINT | NO | FK, UK | 회원 (1:1) |
| refresh_token | TEXT | NO | | Drive OAuth Refresh Token (암호화) |
| drive_root_folder_id | VARCHAR(100) | YES | | 회원 Drive 루트 폴더 |
| token_expires_at | DATETIME | YES | | Access Token 만료 |
| created_at | DATETIME | NO | | 생성일 |
| updated_at | DATETIME | NO | | 수정일 |

**FK:** `member_id` → `member(id)` ON DELETE CASCADE

---

### 3.9 ai_analysis_job (MVP 이후)

| 컬럼 | 타입 | Null | Key | 설명 |
|------|------|------|-----|------|
| id | BIGINT | NO | PK | Auto Increment |
| project_id | BIGINT | NO | FK | Wedding Project |
| requested_by | BIGINT | NO | FK → member.id | 요청자 |
| status | ENUM('PENDING','PROCESSING','COMPLETED','FAILED') | NO | | 작업 상태 |
| total_files | INT | NO | | 분석 대상 파일 수 |
| processed_files | INT | NO | | 처리 완료 파일 수 |
| started_at | DATETIME | YES | | 시작일 |
| completed_at | DATETIME | YES | | 완료일 |
| created_at | DATETIME | NO | | 생성일 |

**FK:** `project_id` → `wedding_project(id)`, `requested_by` → `member(id)`

---

### 3.10 ai_photo_result (1차 · PHOTO/VIDEO 공통)

| 컬럼 | 타입 | Null | Key | 설명 |
|------|------|------|-----|------|
| id | BIGINT | NO | PK | Auto Increment |
| job_id | BIGINT | NO | FK | AI 분석 Job |
| upload_file_id | BIGINT | NO | FK, UK | 분석 대상 파일 (PHOTO 또는 VIDEO, 1:1) |
| scene_category | ENUM('ENTRANCE','SONG','GROUP_PHOTO','RECEPTION','OTHER') | NO | | 장면 분류 (Notion FR-AI-004) |
| is_best_shot | BOOLEAN | NO | | Best Shot 여부 |
| confidence | DECIMAL(5,4) | YES | | AI 신뢰도 |
| metadata | JSON | YES | | 인물·객체·장소, VIDEO 시 frameCount/frameScenes |
| analyzed_at | DATETIME | NO | | 분석일 |

**FK:** `job_id` → `ai_analysis_job(id)`, `upload_file_id` → `upload_file(id)`  
VIDEO는 FFmpeg 프레임 분석 후 장면 다수결로 1행 저장한다.

---

### 3.11 ai_video_job (1차 · 하이라이트 + Drive AI/Archive)

| 컬럼 | 타입 | Null | Key | 설명 |
|------|------|------|-----|------|
| id | BIGINT | NO | PK | Auto Increment |
| project_id | BIGINT | NO | FK | Wedding Project |
| requested_by | BIGINT | NO | FK → member.id | 요청자 |
| status | ENUM('PENDING','PROCESSING','COMPLETED','FAILED') | NO | | 작업 상태 |
| drive_file_id | VARCHAR(100) | YES | | Drive AI/ 파일 ID (Archive에도 동일 파일명 업로드) |
| storage_key | VARCHAR(500) | YES | | ObjectStorage 키 (서버 보관) |
| storage_provider | VARCHAR(50) | YES | | local / gcs |
| file_size | BIGINT | YES | | 바이트 |
| clip_count | INT | NO | | 사용한 Best Shot 수 |
| processed_clips | INT | NO | | 준비된 클립 수 (진행률) |
| options_json | TEXT | YES | | style/length/bgm/subtitles JSON |
| error_message | TEXT | YES | | 오류 메시지 |
| started_at | DATETIME | YES | | 시작일 |
| completed_at | DATETIME | YES | | 완료일 |
| created_at | DATETIME | NO | | 생성일 |

**FK:** `project_id` → `wedding_project(id)`, `requested_by` → `member(id)`

---

## 4. 관계 요약

| 관계 | Cardinality | 설명 |
|------|-------------|------|
| member → oauth_account | 1:N | 1회원, 복수 OAuth (Google + Naver) |
| member → wedding_project | 1:N | 1회원, 복수 Project (MVP: 1개 제한은 앱 레벨) |
| member → drive_connection | 1:1 | Drive OAuth 연동 |
| wedding_project → invitation | 1:1 | Project당 청첩장 1개 |
| wedding_project → invite_link | 1:1 | Project당 초대 링크 1개 |
| wedding_project → upload_file | 1:N | Project당 다수 업로드 |
| wedding_project → guestbook_entry | 1:N | Project당 다수 방명록 |
| upload_file → ai_photo_result | 1:1 | 파일당 AI 결과 1개 |
| ai_analysis_job → ai_photo_result | 1:N | Job당 다수 결과 |

---

## 5. Google Drive 폴더 구조 (참조)

```text
Drive Root (member)
└── Wedding_{slug}/
    ├── Photos/
    │   └── {guest_name}/
    ├── Videos/
    │   └── {guest_name}/
    ├── AI/
    └── Archive/
```

DB에는 `wedding_project.drive_root_folder_id`와 `upload_file.drive_folder_path`로 매핑한다.

---

## 6. FR 매핑

| FR | 관련 테이블 |
|----|-------------|
| FR-MEM-001~006 | member, oauth_account |
| FR-PRJ-001~007 | wedding_project, invite_link |
| FR-INV-001~006 | invitation |
| FR-GST-002~006 | upload_file |
| FR-STR-001~005 | upload_file, drive_connection, wedding_project |
| FR-GBK-001~004 | guestbook_entry |
| FR-AI-001~005, 011 | ai_analysis_job, ai_photo_result |
| FR-AI-006~010 | ai_video_job (+ Drive file id) |
| FR-ADM-002~006 | member, wedding_project, ai_* |

상세 동작: `docs/ai-analysis.md`

---

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|------|------|-----------|--------|
| 1.1.0 | 2026-09-13 | AI 테이블 1차 완료·FR 매핑 갱신 | - |
| 1.0.0 | 2026-09-10 | 초안 작성 | - |
