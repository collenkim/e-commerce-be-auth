# NFR Design Patterns — Unit: RateLimit

## 회복탄력성 패턴: Fail-Open

- `RateLimitService`의 모든 Redis 접근은 try-catch로 감싸고, 예외 발생 시 "허용/차단아님"으로 처리해 반환한다(Q1:A).
- 재시도 없음 — 실패를 감지하는 즉시 fail-open 처리(지연을 늘리지 않음).

## 통합 패턴: Servlet Filter + 직접 호출 하이브리드

- `RateLimitFilter`는 Spring Security의 `SecurityFilterChain`이 아니라 **일반 서블릿 필터**(`FilterRegistrationBean`으로 `/api/auth/signup`, `/api/auth/login`에만 등록)로 구현한다. `SecurityContext`가 필요 없고, Authorization Unit의 향후 필터체인 구성과 독립적으로 동작해야 하기 때문이다(의존성 최소화).
- 계정 기준 검사는 `AccountController`가 `RateLimitService`를 직접 호출한다(business-rules.md 상속).

## 보안 패턴

- SECURITY-11(공개 엔드포인트 rate limiting 요구)을 이 Unit이 충족한다.
- 차단/제한 응답(429/403)은 내부 구현 세부사항을 노출하지 않는 일반 메시지만 반환한다(SECURITY-09).

## 확장성/성능 패턴

- 무상태, Redis 공유 인스턴스 — Token Unit과 동일한 원칙.
