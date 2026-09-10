# 청첩장(Invitation) 기능 설명

> 브랜치: `feat/invitation`  
> 상태: `dev`에 merge 완료 (`feat/invitation`)

## 1. 개요

Wedding Project에 연결된 **모바일 청첩장**을 편집·공개하고, 하객이 `/w/[slug]`로 조회한다.

연관 FR: `FR-INV-001` ~ `FR-INV-005`

## 2. 흐름

```text
신랑·신부
  청첩장 편집 → 저장
  초대 링크 활성 (share)
  공개하기 (published=true)
        │
        ▼
하객 /w/{slug}
  공개 + 링크 활성일 때만 조회
  (업로드 CTA는 다음 단계에서 연결)
```

## 3. Backend API

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/projects/{id}/invitation` | JWT | 소유자 조회 |
| PUT | `/api/projects/{id}/invitation` | JWT | 내용 수정 |
| PATCH | `/api/projects/{id}/invitation/publish` | JWT | `{ published }` |
| GET | `/api/public/w/{slug}` | Public | 하객 조회 |

공개 조건:
1. Project 존재·미삭제
2. InviteLink `active=true`
3. Invitation `published=true`

## 4. Frontend

| 경로 | 역할 |
|------|------|
| `/dashboard/projects/[id]/invitation` | 편집·공개/비공개 |
| `/w/[slug]` | 하객용 모바일 청첩장 |

### 지도
- Project 생성/수정 시 등록한 `venueAddress`를 사용
- 공개 청첩장 **오시는 길**: **카카오맵 JS API**로 지도+마커 표시 + 카카오맵 앱/웹 링크
- 환경 변수: `NEXT_PUBLIC_KAKAO_MAP_APP_KEY` (카카오 개발자 콘솔 JavaScript 키)
- Web 플랫폼 도메인에 `http://localhost:3000` 등록 필요
- 카카오맵 검색 URL 자동 생성: `https://map.kakao.com/?q={주소}`
- 청첩장에서 별도 지도 URL 입력 없음

### 공개 청첩장 UI (`/w/[slug]`)
- 섹션 라벨: 영문 + 골드 헤어라인 (`Greeting` / `Location` / `Account`)
- 인삿말: 카드 없이 중앙 정렬, 넉넉한 행간·자간
- 축의금: 카드 제거, 관계(좌) / 은행·계좌(우) 단정한 가로 리스트
- 예식 정보: 박스 대신 타이포 중심 배치

### 축의금 계좌 (편집)
- 편집: **관계 → 은행 선택 → 계좌번호** / 토스형 은행 피커 / `+ 계좌 추가`는 아래에 append
- JSON 배열로 `invitation.accountInfo`에 저장

## 5. 엔티티 변경

`Invitation.accountInfo`에 계좌 JSON 저장.  
`mapUrl`은 Project 주소 기반 카카오맵 URL로 자동 세팅.

## 6. 다음 확장

- `/w/[slug]/upload` 실제 업로드 + Google Drive
- 갤러리 섹션 JSON
