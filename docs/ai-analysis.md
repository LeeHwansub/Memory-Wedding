# AI 분석 (장면 분류 · Best Shot · 동영상)

> 브랜치: `feat/ai-video-status-regen`  
> 참조: [Notion 요구사항](https://chip-sail-0e6.notion.site/Memory-Wedding-38f14c71cd4f806abfedef0e05f55306) · `docs/requirements-spec.md` FR-AI-001~018

## 1. 개요

하객이 업로드한 **완료된 사진·영상**을 AI Vision으로 분석하고, Notion FR-AI-004 기준 장면별로 분류한 뒤 장면마다 Best Shot을 선정한다.  
영상은 FFmpeg로 균등 프레임을 추출한 뒤 프레임별 분류 → **장면 다수결 + 해당 장면 max confidence**로 집계한다.  
**confidence가 `GEMINI_MIN_CONFIDENCE` 미만인 항목은 장면 분류·Best Shot에서 제외**한다 (원본은 갤러리 / Drive `Photos|Videos/{이름}/`에 그대로 유지).  
`GEMINI_API_KEY`가 없으면 **mock 휴리스틱**으로 동작한다.

## 2. Notion FR 대응

| FR | 기능 | 상태 |
|----|------|------|
| FR-AI-001 | AI 분석 요청 | ✅ 수동 실행 (사진+영상) |
| FR-AI-002 | 사진 분석 (인물·객체·장소) | ✅ |
| FR-AI-003 | 동영상 분석 | ✅ 프레임 추출 + 장면 집계 |
| FR-AI-004 | 장면 분류 | ✅ |
| FR-AI-005 | 대표 장면(Best Shot) 선정 | ✅ 임계값 이상만 |
| FR-AI-006~010 | 영상 생성·Drive·진행·알림·재생성 | ✅ 006~010 1차 (동기 생성 기준) |
| FR-AI-011 | AI 분석 결과 조회 | ✅ |
| FR-AI-012~018 | BGM·자막·스타일 등 | ❌ 후속 |

## 3. 장면 카테고리 (FR-AI-004)

| Enum | 표시 |
|------|------|
| `ENTRANCE` | 입장 |
| `SONG` | 축가 |
| `GROUP_PHOTO` | 단체사진 |
| `RECEPTION` | 피로연 |
| `OTHER` | 기타 |

## 4. 동영상 분석 (FR-AI-003)

1. `COMPLETED` VIDEO 최대 `GEMINI_MAX_VIDEOS`개
2. FFmpeg로 최대 `GEMINI_VIDEO_FRAMES`장 JPEG 균등 추출
3. 프레임마다 Gemini/mock 분류
4. 장면 **다수결**, confidence는 채택 장면의 **최대값**
5. `ai_photo_result`에 1행 저장 (`upload_file` 1:1, VIDEO도 동일 테이블)

`metadata_json` 예시 (영상):

```json
{
  "provider": "gemini",
  "note": "gemini-2.5-flash",
  "people": ["신랑", "신부"],
  "objects": ["부케"],
  "place": "예식장",
  "frameCount": 5,
  "frameScenes": [
    { "index": 0, "category": "ENTRANCE", "confidence": 0.82 },
    { "index": 1, "category": "ENTRANCE", "confidence": 0.77 }
  ]
}
```

FFmpeg 미설치·추출 실패 시: 해당 영상만 파일명 휴리스틱 폴백 (Job 전체 실패 아님).

## 5. 데이터

- `ai_analysis_job` — 요청·상태·처리 건수
- `ai_photo_result` — PHOTO/VIDEO 공통 (`upload_file_id` UNIQUE). VIDEO는 `metadata.frameCount`로 구분

## 6. API

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/projects/{id}/ai` | 최근 분석 Job + 결과 + Best Shot + 하이라이트 Job |
| POST | `/api/projects/{id}/ai/analyze` | 사진·영상 분석 (동기) |
| GET | `/api/projects/{id}/ai/video` | 최근 하이라이트 Job |
| POST | `/api/projects/{id}/ai/video` | Best Shot 사진 슬라이드쇼 MP4 생성 (동기, Drive 미저장) |
| GET | `/api/projects/{id}/ai/video/{jobId}/content` | 생성된 MP4 스트리밍 |

설정:
- `GEMINI_API_KEY` / `GEMINI_MODEL` (default `gemini-2.5-flash`)
- `GEMINI_MAX_PHOTOS` (default 20)
- `GEMINI_MIN_CONFIDENCE` (default 0.60)
- `GEMINI_MAX_VIDEOS` (default 5)
- `GEMINI_VIDEO_FRAMES` (default 5)

대시보드 필드: `minConfidence`, `excludedCount`, `fileType`, `frameCount`

## 7. Frontend

| 경로 | 역할 |
|------|------|
| `/dashboard/projects/[id]/ai` | 분석·Best Shot·장면 그리드·하이라이트 생성/재생 |

## 8. 하이라이트 영상 (FR-AI-006)

1. 최근 분석 Job의 Best Shot 중 **PHOTO**만 사용
2. **예식 흐름 순서:** 입장 → 축가 → 단체사진 → 피로연 → 기타 (같은 장면은 confidence 높은 순)
3. FFmpeg 자동 편집 (2-pass):
   - 장당 **고정 길이 클립** 렌더 (Ken Burns 줌 + 페이드 인/아웃)
   - 클립을 concat (타임스탬프 꼬임으로 긴 검은 화면이 생기던 xfade 단일 그래프 제거)
   - 1280×720, 장당 약 3.2초
   - 최종 길이 ≈ Best Shot 사진 수 × 3.2초
4. `ai_video_job` 저장 + ObjectStorage(`ai-highlight/{projectId}/...mp4`)
5. **Drive 연동 시** `AI/` 와 `Archive/` 에 동일 파일명으로 best-effort 업로드 (`drive_file_id` = AI 폴더 파일 ID). 미연동·실패해도 하이라이트는 COMPLETED 유지.

재생: AI 페이지에서 생성 후 `<video>`로 미리보기.

### 진행 · 완료 안내 · 재생성 (FR-AI-008~010)

| FR | 구현 |
|----|------|
| FR-AI-008 | 생성 중 단계 UI (Best Shot → 렌더 → 합성 → Drive). Job status PENDING/PROCESSING 조회. 생성 중 중복 요청 차단 |
| FR-AI-009 | 완료 시 성공 메시지 + 페이지 내 완료 배너 (푸시/메일 알림은 후속) |
| FR-AI-010 | 「하이라이트 다시 생성」+ 확인 다이얼로그. 기존 COMPLETED가 있어도 새 Job 생성 |

> 현재 생성은 **동기 API**. 단계 UI는 대기 중 UX용이며, 서버 퍼센트 진행률·비동기 Queue는 NFR-003 후속.

## 9. Docker

backend 이미지에 `ffmpeg` 패키지 포함 (`backend/Dockerfile`).

## 10. 후속

- 비동기 Queue + 실 진행률 (NFR-003)
- BGM·자막·스타일 (FR-AI-012~015)
- 푸시/메일 완료 알림
- 영상 클립을 하이라이트에 포함
