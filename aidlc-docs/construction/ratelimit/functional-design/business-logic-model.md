# Business Logic Model — Unit: RateLimit

## 통합 방식: 필터(IP) + 직접 호출(계정) 하이브리드

- **IP 기준 검사(US-401, US-403)**는 요청 본문을 읽을 필요가 없어(IP·경로만 필요) `RateLimitFilter`(서블릿 필터)가 `/api/auth/signup`, `/api/auth/login` 앞단에서 처리한다.
- **계정 기준 검사(US-402)와 브루트포스(US-404)**는 이메일이 요청 본문(JSON)에 있어 필터에서 다시 읽기 까다롭다. 대신 **Account Unit(이미 완료됨)의 `AccountController`가 `RateLimitService`를 직접 호출**하도록 통합한다 — Account Unit이 Token Unit을 호출하는 것과 동일한 패턴(Code Generation에서 `AccountController` 수정).

## 1. IP 기준 요청 수 검사 (필터, US-401)

**절차**: 요청 진입 시 `ratelimit:ip:{endpoint}:{ip}` 카운터를 증가시킨다(없으면 TTL 60초로 생성). 증가 후 값이 임계치(10)를 초과하면 429를 반환하고 컨트롤러로 진행하지 않는다. 이때 `ratelimit:ip-violation:{ip}` 카운터도 함께 증가시킨다(TTL 60초).

## 2. IP 자동 차단 (필터, US-403)

**절차**: 1번에서 `ratelimit:ip-violation:{ip}`가 임계치(3)에 도달하면 `block:ip:{ip}`를 사유 `RATE_LIMIT`, TTL 15분으로 설정한다.

## 3. IP 차단 확인 (필터, 모든 요청 전제 조건)

**절차**: 요청 진입 시 가장 먼저 `block:ip:{ip}` 존재 여부를 확인한다. 있으면 즉시 403(또는 429)을 반환하고 이후 검사/컨트롤러 실행을 건너뛴다.

## 4. 계정 기준 로그인 실패 카운트 및 제한 (Account Unit이 호출, US-402)

**절차**: `AccountController.login()`이 `AccountService.login()`을 호출하기 **전에** `RateLimitService.assertAccountNotBlocked(email)`을 호출한다(차단 중이면 예외 → 429). `AccountService.login()`이 `InvalidCredentialsException`을 던지면(비밀번호 불일치 또는 계정 없음), `RateLimitService.recordLoginFailure(ip, email)`을 호출한 뒤 원래 예외를 다시 던진다. `recordLoginFailure` 내부에서 `ratelimit:account:{email}` 카운터를 증가시키고 임계치(5) 도달 시 `block:account:{email}`을 TTL 15분으로 설정한다.

## 5. 브루트포스 IP 차단 (Account Unit이 호출, US-404)

**절차**: 4번의 `recordLoginFailure(ip, email)` 호출 안에서 함께 처리한다 — `bruteforce:ip-targets:{ip}` Set에 `email`을 추가(TTL 5분 유지/갱신)하고, Set의 카디널리티가 임계치(10)에 도달하면 `block:ip:{ip}`를 사유 `BRUTE_FORCE`, TTL 15분으로 설정한다.

## 6. 자동 해제 (US-405)

별도 절차 없음 — 모든 차단/카운터 키는 Redis TTL로 자연 만료된다.

## 7. 감사 로깅 (사전 결정)

IP 차단 발생 시 사유(`RATE_LIMIT` vs `BRUTE_FORCE`)를 구분해 로그로 남긴다(민감정보 제외, SECURITY-03/14).
