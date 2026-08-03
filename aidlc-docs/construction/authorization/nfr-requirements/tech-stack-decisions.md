# Tech Stack Decisions — Unit: Authorization

| 영역 | 선택 | 근거 |
|---|---|---|
| CORS | Spring Security `CorsConfigurationSource` Bean, 오리진은 `app.social-login.frontend-redirect-uri`에서 파생 | Q1:A |
| 401/403 응답 | 커스텀 `AuthenticationEntryPoint`/`AccessDeniedHandler` | 다른 Unit과 동일한 `{code, message}` 형식 통일 |
| PBT 프레임워크 | jqwik | 상속 |

## Shared Infrastructure 갱신 필요 사항

- 없음.
