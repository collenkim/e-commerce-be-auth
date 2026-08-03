# Code Generation Plan — Unit: RateLimit

**패키지**: `com.ecommerce.auth.ratelimit`
**구현 스토리**: US-401, US-402, US-403, US-404, US-405
**의존 Unit**: 없음(Redis 직접 사용) — 단, Account Unit(완료됨)의 `AccountController`에 계정 기준 검사 호출 추가 필요

## Steps

- [x] Step 0: Account Unit `AccountController` 수정 — `login()`에 `RateLimitService.assertAccountNotBlocked()`(사전) / `recordLoginFailure()`(실패 시) 호출 추가. 기존 `AccountControllerTest` 재실행으로 회귀 없음 확인(+2케이스 추가)
- [x] Step 1: Project Structure Setup — 추가 의존성 불필요(Redis는 Token Unit에서 이미 추가됨)
- [x] Step 2: Business Logic Generation — `BlockReason` enum, `RateLimitProperties`, `RateLimitService`(fail-open), 예외 클래스(`IpBlockedException`, `AccountBlockedException`, `RateLimitExceededException`, `RateLimitExceptionHandler`)
- [x] Step 3: Business Logic Unit Testing — `RateLimitServiceTest`(12케이스)
- [x] Step 4: Business Logic Summary
- [x] Step 5: Filter Layer Generation — `RateLimitFilter`(일반 서블릿 필터), `RateLimitFilterConfig`(`FilterRegistrationBean`, `/api/auth/signup`+`/api/auth/login`)
- [x] Step 6: Filter Layer Unit Testing — `RateLimitFilterTest`(3케이스)
- [x] Step 7: Filter Layer Summary
- [x] Step 8: N/A — Repository 없음(Redis 직접, JPA 리포지토리 불필요)
- [x] Step 9: N/A
- [x] Step 10: N/A
- [x] Step 11: N/A — DB 마이그레이션 불필요(Redis만 사용)
- [x] Step 12: Documentation Generation — `aidlc-docs/construction/ratelimit/code/code-summary.md`, `integration-points.md` 갱신(RateLimit/SocialLogin 미해결 항목 처리 완료로 표시)
- [x] Step 13: Deployment Artifacts — `application.properties`에 `app.rate-limit.*` 설정 추가
