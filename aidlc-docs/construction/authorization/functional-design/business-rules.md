# Business Rules — Unit: Authorization

## 접근 제어 규칙 (Deny by Default, US-603, SECURITY-08)

1. `domain-entities.md`의 공개 엔드포인트 목록만 인증 없이 접근 가능하다.
2. `/api/admin/**`은 인증 + `ADMIN` 역할이 모두 필요하다.
3. 그 외 모든 경로는 인증(유효한 Access Token)이 필요하다.
4. 인증/인가 실패는 각각 401/403으로 구분되어야 한다(Spring Security 기본 동작).

## 관리자 역할 변경 규칙 (US-502)

1. `ADMIN` 역할을 가진 요청자만 호출 가능하다(서버 사이드 강제, 클라이언트 은닉에 의존하지 않음 — SECURITY-08).
2. 대상 계정이 존재하지 않으면 404를 반환한다.
3. 자기 자신의 역할 변경도 허용한다(특별 제한 없음, 단순성 우선).
4. 역할 변경은 즉시 DB에 반영되지만, 이미 발급된 토큰에는 재로그인 전까지 반영되지 않는다(Token Unit 상속 제약, 문서화된 알려진 동작).

## 다른 Unit과의 계약

- `AccountService.changeRole(UUID, Role)` — Account Unit에 신규 추가(Code Generation)
- `JwtAuthenticationFilter` — Token Unit이 이미 생성, 이 Unit이 최초로 배선
- `SocialLoginSecurityConfig.temporaryOpenFilterChain` — 이 Unit이 삭제하고 대체
