# Business Logic Model — Unit: Authorization

## 1. `SecurityFilterChain` 통합 (US-603, 최종 배선)

**절차**:
1. `com.ecommerce.auth.sociallogin.security.SocialLoginSecurityConfig`의 `temporaryOpenFilterChain`(Order 2)을 **삭제**하고, 이 Unit이 그 자리를 대체하는 `SecurityFilterChain`(Order 2)을 만든다. `oauth2LoginFilterChain`(Order 1, SocialLogin Unit 소유)은 그대로 둔다.
2. 새 필터체인은 `JwtAuthenticationFilter`(Token Unit)를 등록해 매 요청마다 Access Token을 검증하고 `SecurityContext`를 채운다.
3. `authorizeHttpRequests` 규칙: 공개 엔드포인트 목록(domain-entities.md)은 `permitAll`, `/api/admin/**`은 `hasRole("ADMIN")`, 그 외는 `authenticated()`.
4. 인증되지 않은 요청이 보호 대상 경로를 호출하면 일관되게 401을 반환한다(US-603).

## 2. 관리자 역할 변경 (US-502, 신규)

**입력**: 대상 accountId, 새 role (USER/SELLER/ADMIN)

**절차**:
1. `JwtAuthenticationFilter`가 이미 요청자의 역할이 ADMIN임을 검증했음을 전제(`authorizeHttpRequests`의 `hasRole("ADMIN")`)
2. `AccountService.changeRole(accountId, newRole)` 호출 — 대상 계정이 없으면 404
3. 성공 시 204 반환

**알려진 제약**: 역할은 Refresh Token 발급 시점에 고정되어 세션 내내 승계된다(Token Unit functional-design 상속) — 관리자가 역할을 변경해도 대상 사용자가 이미 로그인된 세션에는 재로그인 전까지 반영되지 않는다. 자기 자신의 역할을 낮추는 것을 막는 특별 처리는 하지 않는다(단순성 우선 — 관리자가 실수로 스스로를 강등할 수 있는 리스크는 낮은 우선순위 리스크로 수용).

## 3. 미인증 요청 거부 (US-603)

`JwtAuthenticationFilter`가 유효한 인증 정보를 채우지 못한 상태로 보호 대상 경로에 도달하면, Spring Security의 기본 `AuthenticationEntryPoint`가 401을 반환한다(추가 커스터마이징 없이 기본 동작 사용 — SECURITY-09에 맞게 일반 메시지인지 Code Generation에서 확인).
