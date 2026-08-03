# NFR Design Patterns — Unit: SocialLogin

## 세션 무상태화 패턴

- Spring Security `oauth2-client`는 인가 왕복(state/PKCE) 동안 짧은 HTTP 세션을 사용한다.
- 커스텀 `AuthenticationSuccessHandler`가 JWT를 발급한 직후 `request.getSession().invalidate()`로 세션을 즉시 무효화한다 — 이후 이 서비스와의 모든 통신은 JWT 기반이며, 서버 사이드 세션이 남아있지 않도록 한다(무상태 API 설계 원칙 유지).

## 회복탄력성 패턴

- provider 호출 실패 시 재시도 없음(NFR Requirements 상속) — Spring Security 기본 `AuthenticationFailureHandler` 확장으로 즉시 실패 리다이렉트.

## 보안 패턴

- `AuthenticationSuccessHandler`/`AuthenticationFailureHandler`가 세션 기반 인증 완료 처리를 대체한다 — 표준 `SavedRequestAwareAuthenticationSuccessHandler`(세션에 인증 정보 저장) 대신 커스텀 핸들러로 교체해 JWT 발급 + 리다이렉트 + 세션 무효화를 수행한다.
- 연동 확인 엔드포인트(`POST .../link/confirm`)는 Account Unit의 비밀번호 재확인에 의존한다 — 별도 세션/쿠키 없이 요청 본문의 `linkToken` + `password`만으로 상태를 검증한다(무상태 유지).

## 확장성/성능 패턴

- 무상태 애플리케이션 원칙 유지(세션 무효화로 강제).
- 외부 provider 호출은 이 서비스의 확장성에 영향받지 않음(provider 측 용량 문제).
