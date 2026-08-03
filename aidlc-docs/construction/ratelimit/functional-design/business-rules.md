# Business Rules — Unit: RateLimit

## 적용 범위 (Q1:A)

- `/api/auth/signup`, `/api/auth/login`만 적용. 다른 엔드포인트(토큰갱신/로그아웃/이메일재발송/비밀번호재설정요청/소셜로그인)는 이번 범위에서 제외 — `integration-points.md`의 해당 미해결 항목을 "적용 안 함(A안 채택)"으로 확정.

## 임계치 (Q2:A)

| 규칙 | 윈도우/임계치 | 조치 | TTL |
|---|---|---|---|
| IP 요청 수(US-401) | 60초 내 10회 초과 | 429 | 60초 |
| IP 자동 차단(US-403) | 60초 내 429를 3회 받음 | IP 차단 | 15분 |
| 계정 로그인 실패(US-402) | 10분 내 5회 | 계정 로그인 제한 | 15분 |
| 브루트포스 IP(US-404) | 5분 내 서로 다른 계정 10개 이상 대상 | IP 차단(즉시) | 15분 |

## 검사 순서

1. IP 차단 여부(필터, 최우선)
2. IP 요청 수 한도(필터)
3. (로그인만) 계정 차단 여부(Account Unit이 호출)
4. 컨트롤러 로직 실행
5. (로그인 실패 시만) 계정 실패 카운트 + 브루트포스 카운트 갱신(Account Unit이 호출)

## 알고리즘 (Q3:A)

고정 윈도우. 경계 버스트는 알려진 트레이드오프 — 단순성 우선(프로젝트 최소 회복탄력성/복잡도 방침과 일치).

## 자동 해제 (US-405)

모든 카운터/차단 키는 Redis TTL로만 관리한다. 별도 배치 없음.

## 감사 로깅

IP 차단 시 사유(`RATE_LIMIT`/`BRUTE_FORCE`)를 구분해 기록한다. 이메일/IP 등 식별자는 필요한 범위까지만 로그에 남기고, 비밀번호 등 민감정보는 절대 남기지 않는다(SECURITY-03).

## 다른 Unit과의 계약

- `RateLimitService.assertIpNotBlocked(String ip)`, `.assertIpWithinLimit(String ip, String endpoint)` — 필터 내부에서 사용(신규, 이 Unit 전용)
- `RateLimitService.assertAccountNotBlocked(String email)`, `.recordLoginFailure(String ip, String email)` — **Account Unit의 `AccountController`가 호출**(Code Generation에서 `AccountController` 수정 필요, 신규 cross-unit 계약)
