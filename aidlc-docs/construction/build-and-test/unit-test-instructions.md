# Unit Test Execution

## Run Unit Tests

### 1. Execute All Tests
```bash
./gradlew test
```

### 2. Review Test Results
- **Verified result (2026-08-03, clean run)**: **116 tests, 0 failures, 0 errors, 0 skipped**, across **28 test classes**.
- **Test Report Location**: `build/reports/tests/test/index.html` (HTML), `build/test-results/test/*.xml` (JUnit XML, CI-consumable).
- **Coverage tool**: Not configured (no JaCoCo or similar plugin added). If code coverage reporting is desired, add the JaCoCo Gradle plugin — not currently part of this project's tooling.

### 3. Test Breakdown by Unit

| Unit | Test classes | Notable coverage |
|---|---|---|
| Token | `JwtProviderTest`, `JwtProviderPropertyTest`(jqwik, PBT-02), `TokenIssuanceServiceTest`, `TokenBlacklistStoreTest`, `RefreshTokenRepositoryTest`, `TokenControllerTest`, `TokenValidationControllerTest`, `JwtAuthenticationFilterTest` | Issue/refresh/reuse-detection/logout/validate flows, fail-closed Redis retry |
| Account | `AccountServiceTest`, `PasswordPolicyTest`, `PasswordPolicyPropertyTest`(jqwik, PBT-03), 3× `@DataJpaTest` repo tests, `AccountControllerTest`, `EmailEventPublisherTest` | Signup/verify/login/reset flows, RabbitMQ publish incl. failure-swallowing |
| SocialLogin | `SocialLoginServiceTest`, `NormalizingOAuth2UserServiceTest`, 2× `@DataJpaTest` repo tests, `LinkConfirmControllerTest` | Login/signup/link-pending/confirm-link branches, provider attribute parsing |
| RateLimit | `RateLimitServiceTest`, `RateLimitFilterTest` | Fixed-window limits, block triggers, fail-open on Redis error |
| Authorization | `AdminAccountControllerTest`, `CustomAuthenticationEntryPointTest`, `CustomAccessDeniedHandlerTest`, `SecurityFilterChainIntegrationTest` | Role change, 401/403 formatting, real end-to-end security enforcement |
| App-level | `ECommerceBeAuthApplicationTests` | Full Spring context boot (H2 substitutes MariaDB) |

### 4. Fix Failing Tests
If tests fail:
1. Open `build/reports/tests/test/index.html` and identify the failing test class/method.
2. Reproduce locally with `./gradlew test --tests "com.ecommerce.auth.<package>.<ClassName>"`.
3. Fix the underlying code or test fixture.
4. Rerun `./gradlew test` until green.

### Known test-design notes (read before "fixing" these)
- Slice tests (`@WebMvcTest`, `@DataJpaTest`) construct their own `ObjectMapper`/use H2 rather than relying on autoconfigured beans — this project's Spring Boot 4.0.7 setup does not reliably expose a Jackson `ObjectMapper` bean outside the web-slice test infrastructure (discovered during RateLimit Unit Code Generation; see `integration-points.md`).
- `SecurityFilterChainIntegrationTest` and `ECommerceBeAuthApplicationTests` are the **only** full-context tests. They are intentionally kept minimal in count but are the tests most likely to catch cross-unit wiring bugs (two real bugs were caught this way during Code Generation — see `integration-points.md` "발견 및 수정된 버그").
