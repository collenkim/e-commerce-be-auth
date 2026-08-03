# Functional Design Plan — Unit: Authorization

**Unit 책임**: 이 서비스 자체 보호 엔드포인트 역할(RBAC) 검증, 공개 엔드포인트 판단 (`unit-of-work.md` Unit 5, 마지막 Unit)
**관련 스토리**: US-501(RBAC), US-502(관리자 전용 기능), US-603(미인증 요청의 일관된 차단)
**의존 Unit**: Token(`JwtAuthenticationFilter` 배선), SocialLogin(`temporaryOpenFilterChain` 대체), Account(엔드포인트 목록)
**입력 문서**: `integration-points.md`("Authorization Unit이 반드시 해야 할 일" 섹션 — 이미 상당 부분 사전 분석됨)

## 사전 분석 결과 (참고용, 질문 아님)

기존 엔드포인트를 검토한 결과, **현재 코드베이스에는 실제로 "역할 기반으로 보호해야 하는" 엔드포인트가 하나도 없습니다**:
- 가입/로그인/이메일인증/재설정/소셜로그인/연동확인 — 원래 공개
- `/api/auth/token/refresh`, `/api/auth/logout` — Spring Security 세션이 아니라 **요청 본문/헤더의 토큰 자체가 자격증명**이므로 `SecurityFilterChain`에서는 permitAll이어야 하고, 보호는 `TokenIssuanceService` 내부의 토큰 파싱 실패로 이미 이뤄짐(추가 조치 불필요)
- `/internal/tokens/validate` — 의도적으로 인증 없음(Token Unit NFR Q4:A, 내부망 전제)

즉 US-501(RBAC)이 실제로 뭔가를 보호하려면, **이 Unit이 최소 하나의 역할 기반 보호 엔드포인트를 직접 만들어야** US-502("관리자 전용 기능(예: 사용자 관리)")가 검증 가능한 형태가 됩니다. 동시에 `integration-points.md`에 남아있던 "SELLER/ADMIN 승격 경로가 없다" 문제도 해소할 좋은 기회입니다.

## Execution Checklist

- [x] Step A: Resolve business logic questions below (gate)
- [x] Step B: Generate `business-logic-model.md`
- [x] Step C: Generate `business-rules.md`
- [x] Step D: Generate `domain-entities.md`

## Questions (GATE)

### Question 1: 관리자 전용 엔드포인트 범위 (US-502 구체화)
A) **역할 변경 API 하나만** — `PATCH /api/admin/accounts/{accountId}/role` (ADMIN 전용, USER/SELLER/ADMIN 간 변경). `integration-points.md`의 "승격 경로 없음" 문제를 직접 해결. 사용자 목록 조회 등은 범위 밖.

B) **목록 조회 + 역할 변경** — A에 더해 `GET /api/admin/accounts`(페이지네이션, ADMIN 전용, 이메일/역할/상태 요약만 노출)도 포함.

C) 이번 Unit에서는 엔드포인트를 만들지 않고 `JwtAuthenticationFilter` 배선 + 공개 엔드포인트 규칙만 정리한다 — US-501/502는 "메커니즘은 준비되었으나 검증할 구체적 보호 자원은 없음" 상태로 남긴다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: 최종 공개 엔드포인트 목록 확정
`integration-points.md`에서 이미 도출된 아래 목록을 그대로 확정할까요? (모두 `permitAll`, 그 외 전부 인증 필요)

`/api/auth/signup`, `/api/auth/login`, `/api/auth/email/verify`, `/api/auth/email/verify/resend`, `/api/auth/password-reset/request`, `/api/auth/password-reset/execute`, `/api/auth/token/refresh`, `/api/auth/logout`, `/internal/tokens/validate`, `/oauth2/**`, `/login/**`, `/api/auth/social/link/confirm`

A) 그대로 확정

B) 조정 필요 — [Answer]: 뒤에 설명

[Answer]:A

## 사전 결정 사항 (질문 없이 적용)

- **역할 검증 위치**: 이 서비스 자신의 (신규) 관리자 엔드포인트에 대해서만 역할을 검증한다. 다른 백엔드 서비스/별도 게이트웨이의 인가는 이 서비스가 발급한 JWT의 role 클레임을 그쪽이 스스로 해석하는 방식이며 이 Unit의 책임이 아니다(Application Design Q4 연장선).
- **미인증 요청 일관된 차단(US-603)**: 위 목록에 없는 모든 경로는 `JwtAuthenticationFilter`가 채운 인증 정보가 없으면 401로 통일 거부한다.
