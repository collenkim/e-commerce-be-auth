# Domain Entities — Unit: Authorization

새 MariaDB 엔티티 없음 — 역할 변경은 Account Unit의 기존 `Account.role` 필드를 갱신하는 것으로 충분하다.

## 다른 Unit과의 계약 (Code Generation 단계에서 추가/사용)

- **신규**: `AccountService.changeRole(UUID accountId, Role newRole)` — Account Unit에 추가 필요(Code Generation에서 `Account` 엔티티에 `changeRole()` 메서드 + `AccountService`에 위임 메서드 추가)
- **사용**: `AccountService.findById(UUID)`(SocialLogin Unit이 이미 추가함) — 역할 변경 대상 계정 존재 확인에 재사용
- **사용**: `com.ecommerce.auth.token.security.JwtAuthenticationFilter` (Token Unit, 이미 생성됨 — 이 Unit이 처음으로 `SecurityFilterChain`에 등록)

## 공개 엔드포인트 목록 (Q2:A로 확정)

`/api/auth/signup`, `/api/auth/login`, `/api/auth/email/verify`, `/api/auth/email/verify/resend`, `/api/auth/password-reset/request`, `/api/auth/password-reset/execute`, `/api/auth/token/refresh`, `/api/auth/logout`, `/internal/tokens/validate`, `/oauth2/**`, `/login/**`, `/api/auth/social/link/confirm`

그 외 모든 경로는 인증 필요. `/api/admin/**`은 인증 + `ADMIN` 역할 필요.
