# Google Drive 연동

> 브랜치: `feat/google-drive`  
> 상태: 1차 구현 (OAuth 연결 · 폴더 · 사본 동기화)

## 1. 개요

하객 업로드 **원본**은 local/GCS에 보관하고, 신랑·신부 Google Drive에는 **사본**만 전달한다.

```text
하객 업로드 → ObjectStorage(원본)
           → (Drive 연결 시) Drive 사본 + drive_file_id
```

## 2. Drive 폴더 구조

```text
Memory Wedding/                  ← member drive_connection.drive_root_folder_id
  Wedding_{slug}/                ← wedding_project.drive_root_folder_id
    Photos/{guestName}/
    Videos/{guestName}/
    AI/
    Archive/
```

## 3. API

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/drive/status` | JWT | 계정 Drive 연결 여부 |
| GET | `/api/drive/connect-url` | JWT | Google OAuth URL |
| GET | `/api/drive/oauth/callback` | Public | code 교환 후 프론트 리다이렉트 |
| DELETE | `/api/drive/disconnect` | JWT | 연결 해제 (Drive 파일은 유지) |
| GET | `/api/projects/{id}/drive` | JWT | 프로젝트 동기화 현황 |
| POST | `/api/projects/{id}/drive/folders` | JWT | Photos/Videos/AI/Archive 준비 |
| POST | `/api/projects/{id}/drive/sync-pending` | JWT | 미동기화 파일 일괄 사본 |
| POST | `/api/projects/{id}/uploads/{fileId}/sync-drive` | JWT | 단건 사본 |

Scope: `drive.file` (+ openid/email/profile for account email)

## 4. Frontend

| 화면 | 역할 |
|------|------|
| `/dashboard/projects/[id]/edit` | Google Drive **연결·해제** (진행/완료 모달) |
| `/dashboard/projects/[id]/gallery` | Drive **동기화** (진행/완료 모달) |
| `/w/[slug]?from={projectId}` | 로그인 시 미리보기 헤더(프로젝트·대시보드) |

## 5. 환경 변수

```bash
# 기존 Google 로그인 클라이언트 재사용
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...

# Google Cloud Console → OAuth 클라이언트 → 승인된 리디렉션 URI 에 추가
DRIVE_REDIRECT_URI=http://localhost:8080/api/drive/oauth/callback
```

## 6. 보안

- refresh token 은 AES-GCM 으로 암호화 저장 (`drive_connection.refresh_token`)
- 로그인 OAuth(`profile,email`)와 Drive 동의는 **분리** (증분 동의)

## 7. 다음

- Drive 삭제 시 사본도 함께 삭제할지 정책
- 대용량 영상 비동기 큐
- 용량(quota) 표시
