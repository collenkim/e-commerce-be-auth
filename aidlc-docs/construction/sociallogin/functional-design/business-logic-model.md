# Business Logic Model — Unit: SocialLogin

## 0. 인가 시작/콜백 경로 (Spring Security `oauth2-client`, Q1:B)

- `startAuthorization`은 별도 컨트롤러 메서드가 필요 없다 — Spring Security가 자동 제공하는 `/oauth2/authorization/{registrationId}`로 리다이렉트하면 된다(registrationId = `kakao`/`naver`/`google`).
- `handleCallback`도 Spring Security가 자동 제공하는 `/login/oauth2/code/{registrationId}`로 처리된다. 인가코드 교환(토큰 획득)까지 프레임워크가 수행하고, 이후 커스텀 `OAuth2UserService`(사용자 정보 정규화) + `AuthenticationSuccessHandler`(계정 생성/연동 + JWT 발급)에서 이 Unit의 비즈니스 로직을 수행한다.
- OAuth2 인가 흐름 자체(콜백까지의 state/PKCE 검증)는 Spring Security가 내부적으로 짧은 서버 세션을 사용한다 — 이는 인가 왕복 동안만 존재하는 임시 상태이며, 로그인 완료 후 실제 API 인증은 여전히 무상태 JWT로 이뤄진다(세션 기반 API 인증으로 전환하는 것이 아님).

## 1. 사용자 정보 정규화

**입력**: provider별 원시 사용자 정보 응답 (커스텀 `OAuth2UserService`가 수신)

**절차**: provider마다 다른 JSON 구조에서 `providerUserId`, `email`만 추출해 공통 형태로 정규화한다(사전 결정 — 닉네임/사진 등은 수집하지 않음).

## 2. 로그인/가입/연동 판단 (OAuth2 성공 핸들러)

**입력**: provider, providerUserId, email(정규화됨)

**절차**:
1. `(provider, providerUserId)`로 `SocialAccount` 조회
2. **있으면**: 연결된 `accountId`로 즉시 `TokenIssuanceService.issue()` 호출 → 토큰 발급 (US-201/202/203 AC1 "있으면 기존 계정으로 로그인")
3. **없으면**: `AccountService.findByEmail(email)`로 동일 이메일 계정 존재 여부 확인
   - **계정 없음** (신규): `AccountService.createSociallyVerifiedAccount(email, randomPasswordHash)`로 즉시 `ACTIVE` 계정 생성(이메일 인증 생략) → `SocialAccount` 링크 저장 → 토큰 발급 (US-201/202/203 AC1 "없으면 신규 생성")
   - **계정 있음** (동일 이메일의 기존 로컬/타 provider 계정): 즉시 연동하지 않는다 — `PendingSocialLink` 저장(TTL 10분) → 연동 확인 토큰 원문을 클라이언트에 반환(로그인 아님, US-201/202/203 AC2 "계정 연동 정책에 따라 처리")

## 3. 연동 확인 (Confirm Link)

**입력**: 연동 확인 토큰(원문), 기존 계정 비밀번호

**절차**:
1. 해시로 `PendingSocialLink` 조회 — 없음/만료/이미 소비됨 → 오류
2. `AccountService.verifyPassword(existingAccountId, rawPassword)` — 실패 시 오류(이 화면에 도달했다는 것 자체가 계정 존재를 이미 전제하므로, 구체적 "비밀번호 불일치" 오류 반환 가능 — 로그인 시나리오의 계정 비노출 원칙과는 다른 맥락)
3. 성공 시: `SocialAccount` 생성(연동), `PendingSocialLink` consumed 처리, `TokenIssuanceService.issue()` 호출 → 토큰 발급

## 4. 클라이언트로의 결과 전달

- OAuth2 콜백은 브라우저 리다이렉트이므로 JSON 응답 대신, 설정된 프런트엔드 콜백 URL로 **리다이렉트**하며 결과를 URL **fragment**(`#access_token=...&refresh_token=...` 또는 연동 필요 시 `#linkRequired=true&linkToken=...`)에 담아 전달한다. fragment는 서버/프록시 로그나 Referer 헤더로 노출되지 않아 query string보다 안전하다(사전 결정, 설계 근거로 기록).
