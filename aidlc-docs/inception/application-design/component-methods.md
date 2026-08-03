# Component Methods

메서드 시그니처와 입출력 개요. 상세 비즈니스 규칙은 CONSTRUCTION 단계 Functional Design에서 정의한다.

---

## AccountComponent

| 메서드 | 목적 | 입력 | 출력 |
|---|---|---|---|
| `signUp(email, rawPassword)` | 이메일/비밀번호 회원가입 | email, rawPassword | accountId (미인증 상태로 생성) |
| `verifyEmail(verificationToken)` | 이메일 인증 처리 | verificationToken | 계정 활성화 결과 |
| `resendVerificationEmail(accountId)` | 인증 메일 재발송 | accountId | 발행 결과(이벤트) |
| `requestPasswordReset(email)` | 비밀번호 재설정 요청 | email | 재설정 토큰 발급(이벤트) |
| `resetPassword(resetToken, newPassword)` | 비밀번호 재설정 실행 | resetToken, newPassword | 변경 결과 (성공 시 TokenComponent에 전체 Refresh Token 무효화 요청) |
| `findByEmail(email)` | 계정 조회 (SocialLoginComponent 등 내부 협력용) | email | Account 또는 없음 |

---

## SocialLoginComponent

| 메서드 | 목적 | 입력 | 출력 |
|---|---|---|---|
| `startAuthorization(provider)` | 제공자 인가 URL 생성 | provider (KAKAO/NAVER/GOOGLE) | redirect URL |
| `handleCallback(provider, authorizationCode)` | 인가 코드 교환 및 로그인/가입 처리 | provider, authorizationCode | accountId + 로그인 결과 (신규/기존/연동확인 필요) |
| `confirmLinking(accountId, linkToken, existingPassword)` | 동일 이메일 기존 계정과의 연동 확인(Q2:C) | accountId, linkToken, existingPassword | 연동 결과 |

---

## TokenComponent

| 메서드 | 목적 | 입력 | 출력 |
|---|---|---|---|
| `issueTokenPair(accountId, role)` | 로그인 성공 시 토큰 발급 | accountId, role | accessToken, refreshToken |
| `refresh(refreshToken)` | 토큰 회전 | refreshToken | 새 accessToken, refreshToken (기존 토큰 사용 처리) |
| `detectReuseAndRevokeFamily(refreshToken)` | 재사용 탐지 시 토큰 패밀리 무효화 | 이미 사용된 refreshToken | 패밀리 전체 무효화 결과 |
| `logout(accessToken, refreshToken)` | 로그아웃 | accessToken, refreshToken | refreshToken 폐기 + accessToken 블랙리스트 등록 |
| `validate(accessToken)` | 토큰 검증/인트로스펙션 (외부 서비스·게이트웨이 호출용, FR-07) | accessToken | valid 여부, accountId, role, 만료시각 |

---

## RateLimitComponent

| 메서드 | 목적 | 입력 | 출력 |
|---|---|---|---|
| `checkAndRecordByIp(ip, endpoint)` | IP 기준 카운터 확인/증가 | ip, endpoint | 허용 여부 |
| `checkAndRecordByAccount(email)` | 계정 기준 실패 카운터 확인/증가 | email | 허용 여부 |
| `isIpBlocked(ip)` | IP 차단 여부 조회 | ip | blocked 여부, 남은 TTL |
| `blockIp(ip, reason, ttl)` | IP 자동 차단 등록 | ip, reason(RATE_LIMIT/BRUTE_FORCE), ttl | 차단 결과 |

---

## AuthorizationComponent

| 메서드 | 목적 | 입력 | 출력 |
|---|---|---|---|
| `isPublicEndpoint(requestPath)` | 공개 엔드포인트 여부 판단 | requestPath | boolean |
| `requireRole(role, requiredRole)` | 이 서비스 자체 보호 엔드포인트의 역할 검증 | role(토큰 클레임), requiredRole | 허용/403 |
