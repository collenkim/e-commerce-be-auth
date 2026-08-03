# Functional Design Plan — Unit: RateLimit

**Unit 책임**: 인증 엔드포인트 IP/계정 기준 Rate Limit, 자동 IP 차단/해제 (`unit-of-work.md` Unit 4)
**관련 스토리**: US-401(IP 기준), US-402(계정 기준), US-403(IP 초과 차단), US-404(로그인 실패 반복 IP 차단), US-405(차단 자동 해제)
**의존 Unit**: 없음(Redis 직접 사용) — 다만 **어떤 엔드포인트에 필터로 결합할지**는 이 Unit이 결정해야 함 (`integration-points.md` "RateLimit Unit이 반드시 해야 할 일" 참고)
**입력 문서**: `requirements.md`(FR-08, FR-09), `stories.md`, `integration-points.md`

## Execution Checklist

- [x] Step A: Resolve business logic questions below (gate)
- [x] Step B: Generate `business-logic-model.md`
- [x] Step C: Generate `business-rules.md`
- [x] Step D: Generate `domain-entities.md`

## Questions (GATE)

### Question 1: 적용 대상 엔드포인트 확정 (integration-points.md 미해결 항목)
FR-08은 "인증 엔드포인트"라고만 명시합니다. 정확히 어디에 적용할까요?

A) 스토리에 명시적으로 언급된 것만 — 회원가입(`/api/auth/signup`), 로그인(`/api/auth/login`). 나머지(토큰갱신/로그아웃/이메일재발송/비밀번호재설정요청/소셜로그인)는 이번 범위에서 제외.

B) 자격증명 추측/남용이 가능한 모든 엔드포인트로 확장 — A에 더해 `/api/auth/token/refresh`(토큰 추측 방지), `/api/auth/email/verify/resend`, `/api/auth/password-reset/request`(재발송 남용 방지), `/api/auth/social/link/confirm`(연동 비밀번호 추측 방지), `/oauth2/authorization/**`(소셜 로그인 시작 남용 방지). `/api/auth/logout`은 자격증명 추측과 무관해 제외.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: 임계치 구체값
스토리는 "지나치게 많은 요청", "반복되는 로그인 실패"라고만 표현합니다. 구체적 수치가 필요합니다.

A) 표준적 기본값 제안:
  - IP 기준(US-401): 분당 10회 초과 시 429
  - 계정 기준(US-402): 10분 내 5회 로그인 실패 시 해당 계정 로그인 일시 제한(15분)
  - IP 자동 차단(US-403, Rate Limit 반복 초과): 1분 내 3번 429를 받으면 해당 IP를 15분 차단
  - 브루트포스 IP 차단(US-404, 여러 계정 대상 로그인 실패): 동일 IP에서 5분 내 서로 다른 계정으로 10회 이상 로그인 실패 시 즉시 15분 차단

B) 직접 지정 — [Answer]: 뒤에 값을 적어주세요

[Answer]: A

### Question 3: 카운팅 알고리즘
A) 고정 윈도우(Fixed Window) — Redis `INCR` + `EXPIRE`. 구현이 가장 단순하지만 윈도우 경계에서 순간적으로 2배까지 허용될 수 있는 경계 버스트 현상이 있음(예: 창 끝 1초 + 창 시작 1초에 각각 최대치).

B) 슬라이딩 윈도우 로그(Sorted Set 기반) — 정확하지만 메모리/연산 비용이 더 큼.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## 사전 결정 사항 (질문 없이 적용)

- **차단 해제**: Redis TTL 자연 만료로 자동 해제(US-405) — 별도 배치/스케줄러 없음.
- **차단 사유 구분 로깅**: Rate Limit 초과(US-403)와 브루트포스(US-404)는 감사 로그에 사유를 구분해 기록한다(SECURITY-03/14, 민감정보 제외).
