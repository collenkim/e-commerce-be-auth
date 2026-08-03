# Business Rules — Unit: SocialLogin

## 계정 생성/연동 규칙

1. `(provider, providerUserId)`가 이미 연결되어 있으면 항상 그 계정으로 로그인한다.
2. 연결이 없고 이메일이 일치하는 기존 계정도 없으면 새 계정을 만들고 즉시 연동한다.
3. 연결이 없고 이메일이 일치하는 기존 계정이 있으면 **자동 연동하지 않는다** — 사용자가 기존 계정 비밀번호로 확인해야 연동된다(Application Design Q2:C).
4. 소셜 로그인으로 생성된 신규 계정은 provider가 이미 이메일 소유를 확인한 것으로 간주해 이메일 인증 절차 없이 즉시 `ACTIVE`로 생성한다.
5. 소셜 전용으로 생성된 계정의 비밀번호 해시는 사용자가 알 수 없는 무작위 값이다 — 이메일/비밀번호 로그인은 자연히 실패하며, 필요하면 Account Unit의 "비밀번호 재설정" 플로우로 실제 비밀번호를 설정할 수 있다(별도 스키마 변경 없이 기존 메커니즘 재사용).

## 연동 확인 토큰 규칙

1. TTL 10분.
2. 한 계정에 대해 여러 provider의 연동 확인이 동시에 대기 중일 수 있다(제한 없음).
3. 연동 확인 실패(비밀번호 불일치)는 구체적으로 안내한다 — 이미 이 화면 진입 자체가 계정 존재를 전제하므로 로그인 실패 메시지의 비노출 원칙이 적용되지 않는다.

## 수집 정보 최소화

- provider 응답에서 `providerUserId`, `email`만 저장한다. 다른 프로필 정보(닉네임, 프로필 사진 등)는 수집·저장하지 않는다.

## 인가 코드/토큰 검증

- 인가 코드 교환, state/PKCE 검증은 Spring Security `oauth2-client`가 수행한다(Q1:B) — 이 Unit이 직접 검증 로직을 구현하지 않는다.
- 유효하지 않거나 만료된 인가 코드는 Spring Security가 인증 실패로 처리 — 이 Unit은 인증 실패 핸들러에서 일반화된 오류로 프런트엔드에 리다이렉트한다(SECURITY-09).

## 클라이언트 결과 전달

- 성공 시: `{redirectUri}#access_token=...&refresh_token=...`
- 연동 확인 필요 시: `{redirectUri}#linkRequired=true&linkToken=...`
- 실패 시: `{redirectUri}#error=social_login_failed`
