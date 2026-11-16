# 방명록(Guestbook) 기능 설명

> 브랜치: `feat/guestbook`  
> 상태: 구현 완료, commit/push 대기 (사용자 허락 필요)

## 1. 개요

하객이 초대 링크로 **이름 + 축하 메시지**를 남기고, 좋아요를 누르며, 신랑·신부가 목록 조회·삭제한다.  
청첩장(`/w/[slug]`)에도 방명록 카드가 **자동 슬라이드**로 노출된다.

연관 FR: `FR-GBK-001` ~ `FR-GBK-003`  
(`FR-GBK-004` 하객 수정은 MVP 이후)

## 2. 흐름

```text
하객 /w/{slug}
  Guestbook 캐러셀 (**최신 5건**, 약 4.5초 자동 전환) + 좋아요

하객 /w/{slug}/guestbook
  작성 / 목록(페이징 5·10·20) / 좋아요 토글

신랑·신부 /dashboard/projects/{id}/guestbook
  목록(페이징) / soft delete / 좋아요 수 표시
```

## 3. Backend API

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/public/w/{slug}/guestbook?page&size&clientKey` | Public | 페이지 목록 |
| POST | `/api/public/w/{slug}/guestbook` | Public | 작성 `{ guestName, message, clientKey }` |
| PUT | `/api/public/w/{slug}/guestbook/{entryId}` | Public | 본인 글 수정 |
| POST | `/api/public/w/{slug}/guestbook/{entryId}/like` | Public | 좋아요 1회 `{ clientKey }` |
| GET | `/api/projects/{projectId}/guestbook?page&size` | JWT | 소유자 목록 |
| DELETE | `/api/projects/{projectId}/guestbook/{entryId}` | JWT | soft delete |

- `size` 허용값: **5, 10, 20** (그 외는 10)
- 공개 조건: Project 존재 + InviteLink 활성 + Invitation 공개

### 작성 규칙
- **기기(clientKey)당 방명록 1개**만 작성 가능
- 본인 글은 `PUT /api/public/w/{slug}/guestbook/{entryId}` 로 수정
- 좋아요는 **게시물당 기기별 1회** (취소 불가)

### 좋아요
- `guestbook_like (entry_id, client_key)` unique
- 브라우저 `localStorage`에 clientKey 저장
- `guestbook_entry.like_count` 증가만 (토글 해제 없음)

## 4. Frontend

| 경로/컴포넌트 | 역할 |
|---------------|------|
| `/w/[slug]` + `GuestbookCarousel` | 청첩장 내 자동 슬라이드 |
| `/w/[slug]/guestbook` | 작성·페이징·좋아요 |
| `/dashboard/projects/[id]/guestbook` | 관리·삭제·페이징 |
| `GuestbookLikeButton` | ♡/♥ 토글 |

## 5. 엔티티

- `guestbook_entry` — `like_count` 추가
- `guestbook_like` — 디바이스별 좋아요

## 6. 다음 확장

- 하객 작성 직후 수정 → **구현됨** (본인 글)
- 욕설 필터 고도화
- 캐러셀 수동 스와이프
- 다른 기기에서도 본인 글 수정 (계정 연동)
