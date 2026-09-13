# AI 분석 (장면 분류 · Best Shot · 하이라이트)

> 기준 브랜치: `dev` (PR #9~#15 + NFR-003 1차)  
> 참조: [Notion 요구사항](https://chip-sail-0e6.notion.site/Memory-Wedding-38f14c71cd4f806abfedef0e05f55306) · `docs/requirements-spec.md` FR-AI-001~018

## 1. 개요

하객이 업로드한 **완료된 사진·영상**을 AI Vision으로 분석하고, Notion FR-AI-004 기준 장면별로 분류한 뒤 장면마다 Best Shot을 선정한다.  
영상은 FFmpeg로 균등 프레임을 추출한 뒤 프레임별 분류 → **장면 다수결 + 해당 장면 max confidence**로 집계한다.  
**confidence가 `GEMINI_MIN_CONFIDENCE` 미만인 항목은 장면 분류·Best Shot에서 제외**한다 (원본은 갤러리 / Drive `Photos|Videos/{이름}/`에 그대로 유지).  
분류된 사진의 Drive `AI/` 복사본은 만들지 않는다. Drive `AI/`·`Archive/`에는 **하이라이트 MP4만** 저장한다 (FR-AI-007).  
`GEMINI_API_KEY`가 없으면 **mock 휴리스틱**으로 동작한다.

## 2. Notion FR 대응 (구현 현황)

| FR | 기능 | 상태 | 비고 |
|----|------|------|------|
| FR-AI-001 | AI 분석 요청 | ✅ | 수동 실행 (사진+영상), 동기 |
| FR-AI-002 | 사진 분석 | ✅ | people / objects / place |
| FR-AI-003 | 동영상 분석 | ✅ | 프레임 추출 + 장면 집계 |
| FR-AI-004 | 장면 분류 | ✅ | ENTRANCE·SONG·GROUP_PHOTO·RECEPTION·OTHER |
| FR-AI-005 | Best Shot 선정 | ✅ | confidence 임계값 이상만 |
| FR-AI-006 | 하이라이트 영상 생성 | ✅ 1차 | Best Shot **PHOTO** 슬라이드쇼 (Ken Burns) |
| FR-AI-007 | Drive AI/Archive 저장 | ✅ 1차 | 하이라이트 MP4 best-effort |
| FR-AI-008 | 생성 진행 상태 | ✅ 1차 | **비동기 Job + FE 폴링** (`processedFiles` / `processedClips`). Redis Queue는 후속 |
| FR-AI-009 | 생성 완료 안내 | ✅ 1차 | 페이지 내 배너·메시지. 푸시/메일은 후속 |
| FR-AI-010 | 영상 재생성 | ✅ | 확인 다이얼로그 + 새 Job. 생성 중 중복 차단 |
| FR-AI-011 | 분석 결과 조회 | ✅ | 대시보드 장면·Best Shot |
| FR-AI-012~018 | BGM·자막·스타일·길이·중복·감정·인물 | ❌ | 후속 |
| NFR-003 | 비동기 AI 처리 | ✅ 1차 | 인메모리 `@Async` 스레드 풀 (외부 Broker 없음) |

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

| 테이블 | 역할 |
|--------|------|
| `ai_analysis_job` | 분석 요청·상태·처리 건수 |
| `ai_photo_result` | PHOTO/VIDEO 공통 (`upload_file_id` UNIQUE). VIDEO는 `metadata.frameCount`로 구분 |
| `ai_video_job` | 하이라이트 생성 Job · storage · Drive 파일 ID |

상세 스키마: `docs/erd.md` §3.9~3.11

## 6. API

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/projects/{id}/ai` | 최근 분석 Job + 결과 + Best Shot + 하이라이트 Job |
| POST | `/api/projects/{id}/ai/analyze` | 사진·영상 분석 (**비동기** — Job 즉시 반환) |
| GET | `/api/projects/{id}/ai/video` | 최근 하이라이트 Job |
| POST | `/api/projects/{id}/ai/video` | 하이라이트 MP4 생성 (**비동기**) + Drive best-effort |
| GET | `/api/projects/{id}/ai/video/{jobId}/content` | 생성된 MP4 스트리밍 |

> POST는 Job을 `PROCESSING`으로 만들고 즉시 응답한다. 워커(`aiTaskExecutor`)가 처리하며, FE는 `GET /ai`를 폴링한다.
> 분석 진행: `processedFiles / totalFiles` · 하이라이트: `processedClips / clipCount`

설정:

| 변수 | 기본 | 설명 |
|------|------|------|
| `GEMINI_API_KEY` | (없음) | 없으면 mock |
| `GEMINI_MODEL` | `gemini-2.5-flash` | |
| `GEMINI_MAX_PHOTOS` | `20` | |
| `GEMINI_MIN_CONFIDENCE` | `0.60` | 미만이면 분류·Best Shot 제외 |
| `GEMINI_MAX_VIDEOS` | `5` | |
| `GEMINI_VIDEO_FRAMES` | `5` | |

대시보드 필드: `minConfidence`, `excludedCount`, `fileType`, `frameCount`, `latestVideoJob`

## 7. Frontend

| 경로 | 역할 |
|------|------|
| `/dashboard/projects/[id]/ai` | 분석 실행 · Best Shot · 장면 그리드 · 하이라이트 생성/진행/재생/재생성 |

## 8. 하이라이트 영상 (FR-AI-006 · 007)

1. 최근 분석 Job의 Best Shot 중 **PHOTO**만 사용
2. **예식 흐름 순서:** 입장 → 축가 → 단체사진 → 피로연 → 기타 (같은 장면은 confidence 높은 순)
3. FFmpeg 자동 편집 (2-pass):
   - 장당 **고정 길이 클립** 렌더 (Ken Burns 줌 + 페이드 인/아웃)
   - 클립 concat (단일 xfade 그래프는 타임스탬프 꼬임으로 제외)
   - 1280×720, 장당 약 3.2초
4. `ai_video_job` + ObjectStorage(`ai-highlight/{projectId}/...mp4`)
5. Drive 연동 시 `AI/` · `Archive/`에 `highlight-{slug}-{jobId}.mp4` best-effort 업로드  
   (`drive_file_id` = AI 폴더 파일 ID). 미연동·실패해도 Job은 COMPLETED 유지

### 진행 · 완료 · 재생성 (FR-AI-008~010)

| FR | 구현 |
|----|------|
| FR-AI-008 | 생성 중 단계 UI + **서버 진행 카운터 폴링** + PENDING/PROCESSING 중복 요청 차단 |
| FR-AI-009 | 완료 메시지 + 페이지 내 배너 |
| FR-AI-010 | 「하이라이트 다시 생성」확인 후 새 Job |

### 비동기 Queue (NFR-003 1차)

- `@EnableAsync` + `aiTaskExecutor` (core 2 / max 4)
- 커밋 후 `AiAsyncDispatcher`가 분석·하이라이트 워커 실행
- 파일/클립 단위 `REQUIRES_NEW`로 진행률 커밋 → 폴링에 반영
- 외부 Redis/Kafka Queue는 후속

## 9. Docker

backend 이미지에 `ffmpeg` 포함 (`backend/Dockerfile`).  
Gemini env는 `docker-compose.yml` / `.env`로 전달. AI·ffmpeg 변경 후: `docker compose up -d --build backend`

## 10. 관련 문서

| 문서 | 내용 |
|------|------|
| `docs/erd.md` | `ai_*` 테이블 |
| `docs/google-drive.md` | Drive 폴더 (Photos/Videos/AI/Archive) |
| `docs/requirements-spec.md` | FR-AI 전체 · 백로그 |
| `docs/system-flow.md` | MVP vs 확장 범위 |

## 11. 후속 (우선순위 제안)

1. Redis 등 외부 Queue · 재시도/데드레터 (NFR-003 고도화)
2. 하이라이트에 **영상 클립** 포함
3. **FR-AI-012~015** — BGM · 자막 · 스타일 · 길이
4. FR-AI-009 푸시/메일 · FR-AI-016~018
