# Code Generation Plan — Unit: Authorization

**패키지**: `com.ecommerce.auth.authorization`
**구현 스토리**: US-501, US-502, US-603
**의존 Unit**: Token(`JwtAuthenticationFilter` 배선), SocialLogin(임시 필터체인 제거), Account(`changeRole` 메서드 추가)

## Steps

- [x] Step 0a: Account Unit 수정 — `Account.changeRole(Role)`, `AccountService.changeRole(UUID, Role)`, `AccountNotFoundException` 추가(+2 테스트). 기존 `AccountServiceTest` 재실행으로 회귀 없음 확인
- [x] Step 0b: SocialLogin Unit 수정 — `SocialLoginSecurityConfig`에서 `temporaryOpenFilterChain` Bean 제거(이 Unit이 대체). `oauth2LoginFilterChain`(Order 1)은 유지
- [x] Step 1: Project Structure Setup — 추가 의존성 불필요
- [x] Step 2: Business Logic Generation — 없음(계획대로, `AccountService.changeRole` 재사용)
- [x] Step 3: N/A
- [x] Step 4: N/A
- [x] Step 5: Security/API Layer Generation — `AuthorizationSecurityConfig`(최종 `SecurityFilterChain`, Order 2), `CustomAuthenticationEntryPoint`, `CustomAccessDeniedHandler`, `AdminAccountController`, `ChangeRoleRequest` DTO
- [x] Step 6: Security/API Layer Unit Testing — `AdminAccountControllerTest`(3케이스), `CustomAuthenticationEntryPointTest`, `CustomAccessDeniedHandlerTest`, **`SecurityFilterChainIntegrationTest`(계획에 없었으나 필터체인 최초 통합 시점이라 추가, 실버그 발견)**
- [x] Step 7: Layer Summary
- [x] Step 8~11: N/A — 리포지토리/마이그레이션 불필요
- [x] Step 12: Documentation Generation — `aidlc-docs/construction/authorization/code/code-summary.md`, `integration-points.md` 최종 갱신
- [x] Step 13: Deployment Artifacts — 없음(설정 변경 불필요)

## 검증 계획 (이 Unit 완료 후, 전체 서비스 최초 통합 시점)

`./gradlew test` 전체 통과 확인 + `ECommerceBeAuthApplicationTests.contextLoads()`가 실제로 `authorizeHttpRequests` 규칙까지 포함해 문제없이 부팅되는지 확인(지금까지는 이 필터체인 자체가 없었음).
