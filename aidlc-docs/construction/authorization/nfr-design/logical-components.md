# Logical Components — Unit: Authorization

| 논리적 컴포넌트 | 역할 | 비고 |
|---|---|---|
| `AuthorizationSecurityConfig` | `finalSecurityFilterChain`(Order 2) 정의: `JwtAuthenticationFilter` 등록, `authorizeHttpRequests`, CORS, 세션 STATELESS | SocialLogin의 `temporaryOpenFilterChain` 대체 |
| `CustomAuthenticationEntryPoint` | 401 응답을 `{code, message}` 형식으로 작성 | |
| `CustomAccessDeniedHandler` | 403 응답을 `{code, message}` 형식으로 작성 | |
| `CorsProperties`/CORS 설정 | 허용 오리진 관리 | `app.social-login.frontend-redirect-uri`에서 파생 |
| `AdminAccountController` | `PATCH /api/admin/accounts/{accountId}/role` | `ADMIN` 역할 필요 |
| `AdminAccountRoleRequest` (DTO) | 역할 변경 요청 바디 | |

## 큐/캐시 등 인프라 요소

- 없음(신규 인프라 자원 불필요)
