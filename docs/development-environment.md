# 웨딩 플랫폼 프로젝트 — AI 개발환경 및 기술 스택 정리

## 1. 프로젝트 개요

모바일 청첩장과 하객 참여형 사진·영상 아카이빙을 결합한 웨딩 플랫폼을 개발한다.

핵심 방향은 단순한 모바일 청첩장이 아니라 결혼식에 참여한 하객의 사진과 영상을 수집하고, Google Drive에 보관하며, AI를 활용해 장면 분류·Best Shot 선정·웨딩 하이라이트 영상 생성까지 확장하는 것이다.

### 주요 서비스
- 모바일 청첩장
- 신랑·신부 사용자 관리
- 결혼식(Project) 관리
- 하객 사진·동영상 업로드
- 방명록
- Google Drive 파일 저장
- AI 사진 분석 및 장면 분류
- Best Shot 선정
- AI 웨딩 하이라이트 영상
- 관리자 기능

---

## 2. 요구사항 명세서 작성 방향

Flow Chart와 ERD Diagram은 별도로 작성하고, 현재는 기능별 요구사항 명세서를 작성한다.

| 항목 | 설명 |
|---|---|
| No | 요구사항 식별 번호 |
| 업무구분 | 기능의 상위 업무 영역 |
| 기능 | 세부 기능 |
| 사용자 구분 | 해당 기능을 사용하는 사용자 |
| 중요도 | 상 / 중 / 하 |
| 기능유형 | CRUD 등의 기능 분류 |
| 기능설명 | 기능의 구체적인 동작 |
| 기타 | 추가 고려사항 |
| 예외처리 | 오류 및 예외 상황 |

### 사용자 구분
- 신랑·신부 → 일반 사용자
- 관리자 → 관리자
- 하객 → 별도 회원 Role을 만들지 않고 참여 기능 제공

---

## 3. Project(PRJ)의 의미

요구사항에서 `PRJ`는 개발 프로젝트가 아니라 **하나의 결혼식 정보 단위**를 의미한다.

```text
Wedding Project
├── 신랑
├── 신부
├── 결혼식 정보
├── 모바일 청첩장
├── Photos
├── Videos
├── AI
└── Archive
```

회원이 자신의 결혼식 정보를 생성하고 관리하는 구조로 설계한다.

---

## 4. 기술 스택

| 구분 | 기술 | 버전 | 사용 목적 | 적용 영역 | 사용 이유 |
|---|---|---:|---|---|---|
| Frontend | Next.js | 15.x | 웹 애플리케이션 및 페이지 구성 | 모바일 청첩장, 사용자 화면, 관리자 | React 기반 SSR/CSR 활용 |
| Frontend | React | 19.x | 컴포넌트 기반 UI 개발 | 청첩장, 하객 참여, 관리자 UI | 재사용 가능한 컴포넌트 구성 |
| Frontend | TypeScript | 5.x | 정적 타입 검사 | Frontend 전체 | 타입 오류 감소 및 유지보수성 향상 |
| Frontend | Tailwind CSS | 4.x | UI 스타일링 및 반응형 디자인 | 모바일 청첩장, 하객 페이지, 관리자 | 모바일 중심 UI를 빠르게 구성 |
| Backend | Spring Boot | 3.x | REST API 및 비즈니스 로직 | 회원, Project, 청첩장, 하객, AI | 핵심 서버 로직 구현 |
| Backend | Java | 21 LTS | Backend 개발 | Spring Boot 전체 | 안정적인 장기 지원 환경 |
| Backend | Spring Data JPA | 3.x | 데이터 접근 및 ORM | 회원, Project, 청첩장, 방명록, AI | 객체 중심 DB 관리 및 CRUD 감소 |
| Backend | Spring Security | 6.x | 인증 및 인가 | 회원, 관리자, API 접근 제어 | OAuth 및 권한 관리 |
| Database | MySQL | 8.x | 서비스 데이터 저장 | 회원, Project, 청첩장, 업로드, 방명록, AI Metadata | 관계형 데이터 관리 |
| OAuth | Google OAuth 2.0 | OAuth 2.0 | Google 로그인 | 회원가입 및 로그인 | 간편 인증 |
| OAuth | Naver OAuth 2.0 | OAuth 2.0 | Naver 로그인 | 회원가입 및 로그인 | 국내 사용자 접근성 확대 |
| Storage | Google Drive API | v3 | 사진·영상 및 결과물 저장 | 하객 업로드, AI 결과물, Archive | 서버 저장 부담 및 비용 감소 |
| AI | Gemini API | 2.x 계열 | 이미지 분석 및 AI 기능 | 장면 분류, Best Shot, AI 분석 | 이미지 분석 기능 구현 |
| Video | FFmpeg | 7.x | 영상 합성 및 인코딩 | AI 웨딩 영상 | 사진·영상 기반 최종 영상 생성 |
| API | REST API | - | Frontend ↔ Backend 통신 | 전체 서비스 | Frontend/Backend 독립 개발 |
| Deployment | Docker | 28.x | 개발·배포 환경 구성 | Backend, Database | 환경 일관성 확보 |
| Version Control | Git / GitHub | Git 2.x | 버전 관리 | 전체 프로젝트 | 변경 이력 및 협업 관리 |

> 버전은 실제 개발 시작 시 `package.json`, `build.gradle`, Docker 환경에 적용된 정확한 버전으로 최종 갱신한다.

---

## 5. AI 활용 전략

### 개발 과정의 AI

#### ChatGPT — 설계 및 문제 해결
- 요구사항 분석
- 기능 정의
- ERD 검토
- API 설계
- 시스템 아키텍처
- 코드 리뷰
- 리팩토링 방향
- 기술 문서

#### Cursor + Claude — 실제 코딩
- 코드 작성
- 여러 파일 수정
- 리팩토링
- 테스트 코드
- 오류 수정
- 프로젝트 구조 분석

역할을 다음과 같이 분리한다.

```text
ChatGPT
→ 프로젝트 설계자

Cursor + Claude
→ Coding Agent / 실제 개발

MCP
→ AI와 프로젝트 정보 연결
```

---

## 6. Cursor + Claude + MCP 개발환경

```text
                 ChatGPT
             설계 / 분석 / 리뷰
                    │
                    ▼
                 Cursor
                    │
               Claude 모델
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
    Notion MCP   GitHub MCP   MySQL MCP
        │           │           │
        ▼           ▼           ▼
     요구사항       코드          DB
```

MCP는 개발자와 AI Coding Agent 사이에서 프로젝트의 외부 정보와 도구를 연결하는 통로로 사용한다.

---

## 7. MCP 선정

### 1순위 — Notion MCP
현재 프로젝트의 문서가 Notion 중심이므로 가장 우선한다.

활용:
- WBS
- 요구사항 명세
- 기술 스택
- API 문서
- 개발 진행 상황
- 프로젝트 문서 검색

예:
> Notion의 FR-MEM-003 요구사항을 확인하고 현재 구현 상태와 비교해줘.

### 1순위 — GitHub MCP
활용:
- Repository
- Issue
- Pull Request
- 개발 작업
- 코드 변경 흐름

개발 흐름:

```text
Notion 요구사항
       ↓
GitHub Issue
       ↓
Cursor + Claude
       ↓
코드 구현
       ↓
Commit / PR
```

### 2순위 — MySQL MCP
개발 DB에 연결하여 AI가 실제 DB 구조를 확인하도록 한다.

활용:
- 테이블 구조
- Schema
- Entity 관계 검토
- 테스트 데이터
- SQL 조회

보안:
- 개발 DB만 연결
- 운영 DB는 연결하지 않음
- AI 계정은 읽기 권한 중심으로 시작

### 3순위 — Google Drive MCP
운영용 Google Drive는 개발 AI에 직접 연결하지 않는다.

필요할 경우 별도의 개발용 Drive를 사용한다.

```text
Google Drive
├── Production
│   └── MCP 연결하지 않음
│
└── Development
    ├── Test Photos
    ├── Test Videos
    └── Test AI
```

---

## 8. MCP 우선순위

| MCP | 우선순위 | 역할 |
|---|---|---|
| Notion MCP | 1순위 | 요구사항 및 프로젝트 문서 |
| GitHub MCP | 1순위 | 코드 및 개발 관리 |
| MySQL MCP | 2순위 | DB 구조 및 개발 데이터 |
| Google Drive MCP | 3순위 | 개발용 테스트 파일 |
| Custom MCP | 추후 | 프로젝트 전용 자동화 |

처음부터 전부 연결하지 않고 **Notion → GitHub → MySQL** 순서로 연결한다.

---

## 9. 전체 개발환경

```text
                         ChatGPT
                    설계 / 요구사항 / 리뷰
                              │
                              ▼
                       ┌────────────┐
                       │   Notion   │
                       │ WBS / FR   │
                       │ API / Docs │
                       └─────┬──────┘
                             │
                         Notion MCP
                             │
                             ▼
┌────────────────────────────────────────────┐
│                   Cursor                   │
│                                            │
│              Claude Coding Agent           │
│                                            │
│       ┌──────────── MCP ────────────┐      │
│       ▼             ▼               ▼      │
│    Notion         GitHub          MySQL    │
│      MCP            MCP             MCP    │
│       │             │               │      │
│    요구사항         코드             DB     │
│                                            │
│             실제 프로젝트 개발             │
└──────────────────────┬─────────────────────┘
                       │
                       ▼
                 GitHub Repository
                       │
             ┌─────────┴─────────┐
             ▼                   ▼
          Next.js            Spring Boot
                                  │
                                  ▼
                               MySQL
                                  │
                    ┌─────────────┴────────────┐
                    ▼                          ▼
             Google Drive API              Gemini API
                    │                          │
              사진 / 영상 저장              사진 분석
                                               │
                                               ▼
                                            FFmpeg
                                               │
                                               ▼
                                        AI 웨딩 영상
```

---

## 10. 개발 AI와 서비스 AI 분리

### 개발 단계

```text
Cursor
 ├── Claude
 ├── Notion MCP
 ├── GitHub MCP
 └── MySQL MCP
```

AI가 개발에 필요한 프로젝트 컨텍스트를 얻고 코딩하는 환경이다.

### 실제 서비스

```text
Spring Boot
 ├── MySQL
 ├── Google Drive API
 ├── Gemini API
 └── FFmpeg
```

MCP는 서비스 Backend의 필수 구성요소가 아니라 **개발자와 AI Coding Agent를 연결하는 개발환경 도구**로 본다.

---

## 11. AI 서비스 처리 방향

하객 사진을 모두 AI로 재분류하여 별도 파일을 대량 생성하면 처리량과 비용이 커질 수 있다.

기본 저장 구조:

```text
Wedding
├── Photos
│   └── Guest Name
├── Videos
├── AI
└── Archive
```

AI는 원본을 불필요하게 복제하기보다 분석 결과와 선별된 결과를 중심으로 관리한다.

```text
업로드
  ↓
Google Drive 원본 저장
  ↓
Gemini 분석
  ↓
장면 분류
  ├── 신랑 입장
  ├── 신부 입장
  ├── 결혼식
  ├── 하객
  └── 기타
  ↓
Best Shot 선정
  ↓
AI 결과 저장
  ↓
FFmpeg
  ↓
웨딩 하이라이트 영상
```

### 확장 기능
초기 필수 기능은 아니지만 향후 확장 가능:
- 감정 분석 저장
- 이미지 Metadata 저장
- 세부 장면 분류
- 자동 폴더 생성
- AI 추천 사진

---

## 12. AI 비용·성능 원칙

1. 모든 사진을 불필요하게 여러 번 분석하지 않는다.
2. 원본 파일을 불필요하게 복제하지 않는다.
3. AI 분석 결과는 필요한 Metadata 중심으로 저장한다.
4. Best Shot 등 가치가 높은 분석부터 적용한다.
5. 대량 이미지 분석은 비동기 처리를 고려한다.
6. 영상 생성은 사용자가 요청했을 때 실행하는 방식을 고려한다.
7. Google Drive에는 원본과 최종 결과물을 중심으로 저장한다.
8. MVP에서는 AI 기능을 제한하여 비용과 처리 시간을 검증한다.

---

## 13. 결제 및 비용 구조

초기 개발환경은 Cursor 중심으로 구성한다.

```text
Cursor Pro
    │
    ├── Claude 모델
    └── MCP
         ├── Notion
         ├── GitHub
         └── MySQL
```

Claude 웹 서비스의 별도 구독은 처음부터 필수로 추가하지 않고 Cursor에서 Claude를 사용하면서 실제 사용량과 필요성을 확인한다.

비용은 다음과 같이 분리한다.

### 개발 도구
- Cursor
- Cursor에서 사용하는 모델 사용량

### 서비스 AI
- Gemini API 사용량

### Storage
- Google Drive 및 관련 저장공간

---

## 14. 권장 개발 순서

```text
1. 요구사항 명세서 완성
        ↓
2. 화면 구성 및 UI/UX 설계
        ↓
3. ERD 작성
        ↓
4. System Flow Chart 작성
        ↓
5. 기술 스택 확정
        ↓
6. GitHub Repository 생성
        ↓
7. Cursor 개발환경 구성
        ↓
8. Claude 모델 적용
        ↓
9. Notion MCP 연결
        ↓
10. GitHub MCP 연결
        ↓
11. MySQL MCP 연결
        ↓
12. Frontend / Backend 기본 구조
        ↓
13. 회원관리
        ↓
14. 결혼식(Project) 관리
        ↓
15. 청첩장
        ↓
16. 하객 참여 / 사진·영상 업로드
        ↓
17. Google Drive 연동
        ↓
18. 방명록
        ↓
19. Gemini AI 분석
        ↓
20. FFmpeg 영상 생성
        ↓
21. 관리자 기능
        ↓
22. 테스트 / 리팩토링
        ↓
23. 배포
```

---

## 15. 최종 방향

### 개발 AI

**ChatGPT**
- 설계
- 분석
- 리뷰
- 문제 해결

**Claude + Cursor**
- 코드 작성
- 리팩토링
- 테스트
- 실제 프로젝트 작업

### MCP

**Notion MCP**
→ 요구사항 및 문서

**GitHub MCP**
→ 코드 및 개발 관리

**MySQL MCP**
→ DB 구조 및 개발 데이터

**Google Drive MCP**
→ 필요 시 개발용 Drive에 한정

### 서비스 AI

**Gemini API**
→ 이미지 분석 / 장면 분류 / Best Shot

**FFmpeg**
→ 영상 합성 / 인코딩

### 핵심 목표

> ChatGPT를 요구사항과 아키텍처 설계에 활용하고, Cursor + Claude를 Coding Agent로 사용하며, MCP를 통해 Notion·GitHub·MySQL의 프로젝트 컨텍스트를 연결하는 AI-assisted Development 환경을 구축한다.

서비스에서는 Google Drive + Gemini API + FFmpeg를 이용하여 하객 사진·영상을 수집하고 분석하여 웨딩 추억을 아카이빙한다.
