# AI 분석 (장면 분류 · Best Shot)

> 브랜치: `feat/ai-confidence-filter`  
> 참조: [Notion 요구사항](https://chip-sail-0e6.notion.site/Memory-Wedding-38f14c71cd4f806abfedef0e05f55306) · `docs/requirements-spec.md` FR-AI-001~018

## 1. 개요

하객이 업로드한 **완료된 사진**을 AI Vision으로 분석하고, Notion FR-AI-004 기준 장면별로 분류한 뒤 장면마다 Best Shot을 선정한다.  
**confidence가 `GEMINI_MIN_CONFIDENCE` 미만인 사진은 장면 분류·Best Shot에서 제외**한다 (원본은 갤러리 / Drive `Photos/{이름}/`에 그대로 유지).  
`GEMINI_API_KEY`가 없으면 **mock 휴리스틱**으로 동작해 로컬에서도 흐름을 검증할 수 있다.

## 2. Notion FR 대응 (1차 구현)

| FR | 기능 | 상태 |
|----|------|------|
| FR-AI-001 | AI 분석 요청 | ✅ 수동 실행 |
| FR-AI-002 | 사진 분석 (인물·객체·장소) | ✅ `people` / `objects` / `place` |
| FR-AI-003 | 동영상 분석 | ❌ 후속 |
| FR-AI-004 | 장면 분류 | ✅ Notion 카테고리 |
| FR-AI-005 | 대표 장면(Best Shot) 선정 | ✅ 장면별 confidence 최대 (임계값 이상만) |
| FR-AI-006~010 | 영상 생성·Drive·진행·알림·재생성 | ❌ 후속 |
| FR-AI-011 | AI 분석 결과 조회 | ✅ |
| FR-AI-012~018 | BGM·자막·스타일·중복제거 등 | ❌ 후속 |

## 3. 장면 카테고리 (FR-AI-004)

| Enum | 표시 |
|------|------|
| `ENTRANCE` | 입장 |
| `SONG` | 축가 |
| `GROUP_PHOTO` | 단체사진 |
| `RECEPTION` | 피로연 |
| `OTHER` | 기타 |

## 4. 사진 분석 메타데이터 (FR-AI-002)

`ai_photo_result.metadata_json` 예시:

```json
{
  "provider": "gemini",
  "note": "gemini-2.5-flash",
  "people": ["신랑", "신부"],
  "objects": ["부케", "촛불"],
  "place": "예식장"
}
```

## 5. 데이터

- `ai_analysis_job` — 요청·상태·처리 건수
- `ai_photo_result` — 파일당 장면·confidence·bestShot·metadata (`upload_file_id` UNIQUE)

## 6. API

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/projects/{id}/ai` | 최근 Job + 결과 + Best Shot |
| POST | `/api/projects/{id}/ai/analyze` | 분석 실행 (동기, 최대 N장) |

설정:
- `app.gemini.api-key` ← `GEMINI_API_KEY`
- `app.gemini.model` ← `GEMINI_MODEL` (default `gemini-2.5-flash`)
- `app.gemini.max-photos` ← `GEMINI_MAX_PHOTOS` (default 20)
- `app.gemini.min-confidence` ← `GEMINI_MIN_CONFIDENCE` (default `0.60`)

대시보드 응답 추가 필드:
- `minConfidence` — 적용 중인 임계값
- `excludedCount` — 임계값 미만으로 분류에서 빠진 장수
- `results` / `bestShots` — 임계값 **이상**만 포함

## 7. Frontend

| 경로 | 역할 |
|------|------|
| `/dashboard/projects/[id]/ai` | 분석 실행·Best Shot·장면별 그리드·제외 건수 안내 |

Drive `AI/` 폴더로 장면별 복사는 하지 않는다. 원본은 기존 `Photos/{guest}/` 경로만 사용한다.

## 8. 후속

- FR-AI-003 동영상 장면 추출
- FR-AI-006~010 FFmpeg 하이라이트 영상 + Drive 결과물 저장
- 비동기 Queue (NFR-003)
- FR-AI-012~018 BGM·자막·스타일·중복 제거 등
