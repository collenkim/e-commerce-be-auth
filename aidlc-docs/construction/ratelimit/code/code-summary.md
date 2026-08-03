# Code Generation Summary — Unit: RateLimit

## Step 0: Account Unit `AccountController` 수정

- `login()`에 `RateLimitService.assertAccountNotBlocked(email)`(사전 검사) + 실패 시 `recordLoginFailure(ip, email)` 호출 추가
- `RateLimitExceptionHandler` 신설 — `AccountBlockedException` → 429
- 회귀 확인: `AccountControllerTest` 재실행(+ 신규 케이스 2개 추가: 실패 시 `recordLoginFailure` 호출 검증, 계정 차단 시 429), 모두 통과

## 생성된 파일

**패키지**: `com.ecommerce.auth.ratelimit`

- `BlockReason.java`, `RateLimitProperties.java`, `RateLimitService.java`(fail-open)
- `exception/{IpBlockedException,RateLimitExceededException,AccountBlockedException,RateLimitExceptionHandler}.java`
- `filter/{RateLimitFilter,RateLimitFilterConfig}.java` — 일반 서블릿 필터, `/api/auth/signup`+`/api/auth/login`에만 등록

### 리포지토리/마이그레이션
없음 — Redis 전용, JPA/Flyway 변경 없음.

### 설정
- `application.properties` — `app.rate-limit.*`(임계치/윈도우/차단시간)

### 테스트
- `RateLimitServiceTest`(12케이스: 정상/한도초과/차단트리거/계정차단/브루트포스/각각의 fail-open 경로)
- `RateLimitFilterTest`(3케이스: IP차단/한도초과/정상통과)
- `AccountControllerTest`에 2케이스 추가

전체 103개 테스트, `./gradlew test` 통과.

## ⚠️ Code Generation 중 발견한 실제 버그 (수정 완료)

`RateLimitFilterConfig`가 Spring Bean으로 등록된 `ObjectMapper`를 생성자 주입받으려 했으나, **전체 애플리케이션 컨텍스트(`ECommerceBeAuthApplicationTests.contextLoads()`)를 실제로 띄워보니 `NoSuchBeanDefinitionException`으로 부팅 자체가 실패**했다. 슬라이스 테스트(`@WebMvcTest` 등)에서는 잡히지 않던 문제 — 이번에 처음으로 전체 컨텍스트 테스트가 통과하는 시점이라 발견됨. `RateLimitFilter`가 자체 `ObjectMapper` 인스턴스를 직접 생성하도록 수정해 해결(TokenControllerTest 등에서 이미 썼던 패턴과 동일).

## ✅ 이전 Unit이 남긴 미검증 위험 해소

`ECommerceBeAuthApplicationTests.contextLoads()`가 이번에 처음 통과했다 — **SocialLogin Unit code-summary.md에서 미검증으로 남겼던 "빈 client-id로 OAuth2 ClientRegistration을 만들 때 부팅이 실패할 수 있다"는 우려가 기우였음을 확인**했다(실패하지 않음). `integration-points.md`에서 해당 항목 해소로 표시.

## `integration-points.md` 갱신

- "RateLimit Unit이 반드시 해야 할 일" 섹션의 항목들을 모두 처리 완료로 표시 — signup/login에는 IP+계정 기준 모두 적용, 나머지 엔드포인트(refresh/logout/재발송/재설정요청/연동확인)는 Functional Design Q1:A 결정에 따라 이번 범위에서 명시적으로 제외.
- "Build and Test 단계에서 검증 필요" 섹션의 OAuth2 항목을 해소로 표시.
