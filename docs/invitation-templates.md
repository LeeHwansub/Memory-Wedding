# 청첩장 템플릿

> 브랜치: `feat/invitation-templates`  
> 연관 FR: `FR-INV-006`

## 1. 개요

사전 정의된 **스킨 템플릿**을 선택하면 색 토큰과 기본 레이아웃 프리셋이 적용된다.  
사진·문구 콘텐츠는 유지되고, 적용 후에도 세부 설정은 다시 조정할 수 있다.

## 2. 템플릿 목록

| ID | 이름 | 특징 | 기본 프리셋 |
|----|------|------|-------------|
| `CLASSIC` | Classic | 골드 포인트 (기본) | TOP + SLIDER |
| `IVORY` | Ivory | 담백·미니멀 | TOP + VERTICAL |
| `NOIR` | Noir | 다크 톤 | TOP + SLIDER |
| `SAGE` | Sage | 세이지 그린 | MIDDLE + COLLAGE 3열 |
| `ROSE` | Rose | 소프트 로즈 | MIDDLE + SLIDER |

## 3. 데이터 / API

- `invitation.template` (`InvitationTemplate` enum, default `CLASSIC`)
- `PUT /api/projects/{id}/invitation` body에 `template` 포함
- owner/public response에 `template` 포함

## 4. Frontend

- `src/lib/invitation-templates.ts` — 토큰·프리셋 정의
- 디자인 페이지 상단 템플릿 선택 UI
- 공개 `/w/[slug]` · 디자인 미리보기에 CSS 변수 적용  
  (`--inv-bg`, `--inv-fg`, `--inv-accent`, `--inv-muted`, `--inv-soft`, `--inv-line`, `--inv-hero-overlay`)
