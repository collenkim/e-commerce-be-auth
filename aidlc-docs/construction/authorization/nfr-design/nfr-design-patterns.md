# NFR Design Patterns — Unit: Authorization

## 필터체인 완성 패턴

- `oauth2LoginFilterChain`(Order 1, SocialLogin Unit 소유)은 그대로 유지.
- 이 Unit이 `finalSecurityFilterChain`(Order 2)을 생성해 SocialLogin의 `temporaryOpenFilterChain`을 **대체**한다(같은 Bean 이름을 새로 정의하는 것이 아니라, SocialLogin 쪽 코드에서 그 Bean 정의 자체를 제거하고 이 Unit의 Config로 옮긴다).
- `JwtAuthenticationFilter`(Token Unit)를 `UsernamePasswordAuthenticationFilter` 앞에 등록.

## 오류 응답 일관성 패턴

- `CustomAuthenticationEntryPoint`(401), `CustomAccessDeniedHandler`(403)를 등록해 `{code, message}` JSON으로 통일 — 다른 Unit의 `@RestControllerAdvice` 패턴과 형식을 맞추되, Spring Security 예외는 `@RestControllerAdvice`로 잡히지 않으므로 `AuthenticationEntryPoint`/`AccessDeniedHandler`로 직접 응답을 작성해야 한다.

## CORS 패턴

- `CorsConfigurationSource` Bean으로 허용 오리진(프런트엔드 리다이렉트 URL에서 파생)만 명시, 자격증명 포함 여부는 Authorization 헤더 기반이라 쿠키 불필요 — `allowCredentials(false)`.

## 확장성/성능 패턴

- 무상태 유지 — 필터체인 자체가 세션을 생성하지 않음(`SessionCreationPolicy.STATELESS`), OAuth2 로그인 왕복 중에만 예외적으로 세션 사용(SocialLogin Unit이 이미 결정).
