# User Stories 생성 계획 (Story Generation Plan)

**역할**: Product Owner 관점에서 작성

## 실행 체크리스트

- [x] Step A: 아래 질문에 대한 답변 수집
- [x] Step B: 답변의 모호성 분석 (필요 시 후속 질문) — Question 2 후속 질문 완료
- [x] Step C: `aidlc-docs/inception/user-stories/personas.md` 생성 — 페르소나 정의
- [x] Step D: `aidlc-docs/inception/user-stories/stories.md` 생성 — INVEST 기준을 만족하는 사용자 스토리 + 인수 조건(Acceptance Criteria)
- [x] Step E: 페르소나 ↔ 스토리 매핑
- [x] Step F: 완료 메시지 제시 및 승인 대기

## 최종 결정 사항 (Finalized)

| 항목 | 결정 |
|---|---|
| 스토리 분해 방식 | Feature 기반 하이브리드 — 기능 단위 Epic(로그인/회원가입, 소셜 로그인, 토큰 관리, Rate Limit/IP 차단, RBAC/인가, API 게이트웨이 라우팅) 아래 필요 시 페르소나별 스토리 배치 |
| 페르소나 (4개) | 구매자(USER), 판매자(SELLER), 관리자(ADMIN), 미인증 방문자(비로그인 사용자) |
| 스토리 입도 | 적당히 세분화 — 소셜 프로바이더별, 토큰 동작(발급/갱신/무효화)별, 차단 시나리오별로 분리 |
| 인수 조건 형식 | Given-When-Then (BDD 스타일) |
| 우선순위 표기 | MoSCoW (Must/Should/Could/Won't) |

## 스토리 분해 방식 옵션 (참고)

| 방식 | 설명 |
|---|---|
| User Journey 기반 | 회원가입→로그인→이용→로그아웃 등 사용자 흐름 순서로 구성 |
| Feature 기반 | 로그인, 소셜 로그인, Rate Limit/차단, 토큰 관리 등 기능 단위로 구성 |
| Persona 기반 | 구매자/판매자/관리자 등 사용자 유형별로 구성 |
| Domain 기반 | 인증(Authentication) / 인가(Authorization) / 보안·어뷰징 방지 등 도메인별로 구성 |
| Epic 기반 | 상위 Epic 아래 하위 스토리를 계층 구조로 구성 |
| 하이브리드 | 위 방식들을 조합 (예: Feature를 Epic으로 묶고, 그 아래 Persona별 스토리 배치) |

---

## 명확화 질문 (Clarification Questions)

아래 질문에 `[Answer]:` 태그 뒤에 알파벳으로 답변해 주세요.

### Question 1 — 스토리 분해 방식
어떤 방식으로 스토리를 구성할까요?

A) Feature 기반 — 로그인/회원가입, 소셜 로그인, 토큰 관리(Refresh/로그아웃), Rate Limit/IP 차단, RBAC/인가, API 게이트웨이 라우팅 등 기능 단위로 Epic을 나누고, 그 안에 필요한 경우 페르소나별 스토리를 배치 (하이브리드)

B) Persona 기반 — 구매자/판매자/관리자별로 각각의 전체 스토리 묶음을 구성

C) User Journey 기반 — 가입→인증→인가→로그아웃 흐름 순서로 구성

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

### Question 2 — 페르소나 확정
`requirements.md`에서 확인된 역할은 구매자(USER)/판매자(SELLER)/관리자(ADMIN) 3개입니다. 스토리 작성 시 이 3개 페르소나로 충분할까요?

A) 예, 3개 페르소나(구매자/판매자/관리자)로 충분함

B) 아니오, 추가 페르소나가 필요함 (예: "미인증 방문자(비로그인 사용자)", "다른 마이크로서비스(시스템 행위자)" 등) — Other에 상세 기재

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

### Question 3 — 스토리 크기/입도(Granularity)
스토리를 얼마나 세분화할까요?

A) 큼직하게 — 기능 단위로 큰 스토리 몇 개 (예: "소셜 로그인 전체"가 스토리 1개)

B) 적당히 세분화 — 각 소셜 프로바이더, 각 토큰 동작(발급/갱신/무효화), 각 차단 시나리오를 별도 스토리로 분리 (INVEST의 Small 기준에 더 부합)

C) 매우 세분화 — 모든 예외 케이스까지 개별 스토리로 분리

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

### Question 4 — 인수 조건(Acceptance Criteria) 형식
인수 조건을 어떤 형식으로 작성할까요?

A) Given-When-Then (BDD 스타일)

B) 단순 체크리스트 (- [ ] 형식의 조건 나열)

C) 두 방식 혼합 — 복잡한 시나리오는 Given-When-Then, 단순한 것은 체크리스트

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

### Question 5 — 우선순위 표기
각 스토리에 우선순위(예: MoSCoW: Must/Should/Could/Won't)를 표기할까요?

A) 예, MoSCoW 방식으로 표기

B) 아니오, 우선순위 표기 불필요 (모든 스토리가 이번 단계에 동등하게 필요)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---
