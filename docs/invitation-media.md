# 청첩장 미디어 (메인 사진 · 웨딩 갤러리)

> 브랜치: `feat/invitation-media`  
> 상태: 구현 중

## 1. 개요

청첩장에 **메인(히어로) 사진 1장**과 **웨딩 갤러리(최대 15장)** 을 올린다.  
신랑·신부 개별 슬롯은 두지 않고, 개별 사진도 **갤러리에 포함**한다.

연관: `docs/invitation.md`

## 2. 결정 사항

| 항목 | 결정 |
|------|------|
| 미디어 종류 | `MAIN` / `GALLERY` 만 |
| 신랑·신부 개별 사진 | 별도 필드 없음 → 갤러리에 넣음 |
| 갤러리 레이아웃 | `SLIDER` / `COLLAGE` / `VERTICAL` (소유자 선택) |
| 메인 배치 | `TOP`(풀블리드+이름 오버레이) / `MIDDLE`(중간 프레임) |
| 메인 보정 | 밝기·채도(0.5~1.5), 초점 X/Y(0~100), 표시 크기 |
| 저장소 | 기존 `ObjectStorage` (로컬/GCS). Drive와 무관 |

## 3. 데이터

### `invitation` 추가 컬럼
- `gallery_layout`, `gallery_columns`, `gallery_image_size`
- `main_photo_size`, `main_photo_placement`
- `main_brightness`, `main_saturation`, `main_focal_x`, `main_focal_y`

### `invitation_media`
- `media_type`, `original_filename`, `mime_type`, `file_size`
- `storage_key`, `storage_provider`, `sort_order`, `deleted_at`

## 4. API

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/projects/{id}/invitation/media?type=MAIN\|GALLERY` | JWT | multipart `file` |
| DELETE | `/api/projects/{id}/invitation/media/{mediaId}` | JWT | soft delete + 스토리지 삭제 |
| PUT | `/api/projects/{id}/invitation/gallery/order` | JWT | `{ galleryIds: [...] }` |
| GET | `/api/projects/{id}/invitation/media/{mediaId}/content` | JWT | 원본 스트림 |
| GET | `/api/public/w/{slug}/media/{mediaId}/content` | Public | 공개 스트림 |
| PUT | `/api/projects/{id}/invitation` | JWT | 미디어 설정 필드 포함 |

제한: 사진 JPG/PNG/WEBP, 10MB↓, 갤러리 최대 15장. MAIN 재업로드 시 기존 MAIN soft-delete.

## 5. Frontend

| 경로 | 역할 |
|------|------|
| `/dashboard/projects/[id]/invitation` | 제목·인사말·계좌·공개 |
| `/dashboard/projects/[id]/invitation/design` | 메인/갤러리·레이아웃 (좌 설정 / 우 미리보기) |
| `/w/[slug]` | 하객용 — 갤러리는 **카드 → 모달** |

컴포넌트:
- `InvitationDesignPreview` — 디자인 페이지 폰 프레임 미리보기
- `GalleryCard` — 공개/미리보기용 카드 + 모달 뷰어
- `MainPhotoHero` — TOP/MIDDLE
- `WeddingGallery` — 모달 안 SLIDER / COLLAGE / VERTICAL
- `InvitationImage` — 공개 URL 또는 JWT blob

## 6. 공개 갤러리 UX

인라인으로 길게 펼치지 않고, 썸네일 카드만 노출.  
클릭 시 모달에서 선택한 레이아웃으로 전체 갤러리를 본다.

## 7. 레이아웃 동작 (모달 내부)

- **SLIDER**: 큰 현재 사진 + 썸네일 스트립
- **COLLAGE**: CSS columns 모자이크 (2~4열)
- **VERTICAL**: 세로 풀폭 스택
