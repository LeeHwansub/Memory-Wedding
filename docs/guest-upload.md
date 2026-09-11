# 하객 업로드 (Guest Upload)

> 브랜치: `feat/guest-upload`  
> 상태: 1차 완료 (Drive 사본 전달은 `feat/google-drive`)

## 1. 개요

하객이 청첩장 링크로 **사진·영상을 업로드**하고, 원본은 **우리 저장소(GCS 또는 로컬)** 에 보관한다.  
신랑·신부 Google Drive **사본 전달**은 `feat/google-drive`에서 이어간다.

연관 FR: `FR-GST-002` ~ `FR-GST-006`, `FR-STR-001`(원본 보관 일부)

## 2. 저장 전략

```text
하객 업로드
  → ObjectStorage (local | gcs)  ← 원본 백업
  → DB upload_file 메타데이터
  → (이후) Google Drive 사본
```

| provider | 설정 | 용도 |
|----------|------|------|
| `local` (기본) | `STORAGE_TYPE=local` | 로컬/Docker 볼륨 개발 |
| `gcs` | `STORAGE_TYPE=gcs` + 버킷·자격증명 | 운영 원본 백업 |

실수로 Drive에서 지워도 GCS/로컬 원본으로 복구 가능.

## 3. API

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/public/w/{slug}/upload` | Public | multipart `guestName` + `file` |
| GET | `/api/public/w/{slug}/uploads` | Public | 완료 파일 목록 |
| GET | `/api/projects/{id}/uploads/folders` | JWT | 사진/영상 → 하객 폴더 트리 |
| GET | `/api/projects/{id}/uploads` | JWT | 소유자 목록 (`fileType`+`guestName` 필터 가능) |
| GET | `/api/projects/{id}/uploads/{fileId}/content` | JWT | 원본 스트리밍 |
| DELETE | `/api/projects/{id}/uploads/{fileId}` | JWT | soft delete + storage 삭제 |

소유자 갤러리 UX (가상 폴더):

```text
업로드 갤러리
├── 사진
│   ├── 김하객
│   └── 이친구
└── 영상
    └── 김하객
```

DB 폴더 엔티티 없이 `guestName` + `fileType`으로 묶는다.

공개 조건: 초대 링크 활성 + 청첩장 공개 (방명록과 동일)

제한:
- 사진: JPG/PNG/WEBP/HEIC ≤ 30MB
- 영상: MP4/MOV/WEBM ≤ 300MB

## 4. Frontend

| 경로 | 역할 |
|------|------|
| `/w/[slug]/upload` | 하객 다중 업로드 |
| `/dashboard/projects/[id]/gallery` | 소유자 갤러리 (사진/영상 → 하객 → 파일) |

## 5. 환경 변수

```bash
STORAGE_TYPE=local          # or gcs
STORAGE_LOCAL_PATH=./data/uploads
GCS_BUCKET=your-bucket
# 로컬 Docker: secrets/ 에 SA JSON 두고 gitignore (이미 /secrets/)
GCS_CREDENTIALS_HOST_PATH=./secrets
GCS_CREDENTIALS_PATH=/secrets/your-sa.json   # 컨테이너 경로
```

## 6. 다음

- Drive 삭제 정책·비동기 큐·용량 표시 → `docs/google-drive.md`
- 공개 청첩장에 갤러리 미리보기
- 영상 썸네일
