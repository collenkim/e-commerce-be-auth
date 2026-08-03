# Domain Entities — Unit: SocialLogin

## SocialAccount (MariaDB)

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | UUID/PK | |
| `provider` | enum (`KAKAO`\|`NAVER`\|`GOOGLE`) | |
| `providerUserId` | string | provider가 발급하는 사용자 고유 ID |
| `accountId` | FK (Account Unit) | |
| `linkedAt` | timestamp | |

**제약**: `(provider, providerUserId)` unique — 하나의 소셜 아이덴티티는 하나의 계정에만 연결. 하나의 `Account`가 여러 provider를 동시에 연결하는 것은 허용(제약 없음).

## PendingSocialLink (MariaDB)

동일 이메일의 기존 계정이 발견됐을 때, 사용자 확인(Q2:A) 전까지 임시로 보관하는 연동 요청.

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | UUID/PK | |
| `existingAccountId` | FK (Account Unit) | 연동 대상 기존 계정 |
| `provider` | enum | |
| `providerUserId` | string | |
| `email` | string | provider가 반환한 이메일 (정규화됨) |
| `tokenHash` | string, unique | 연동 확인 토큰(원문)의 해시. `OpaqueTokenGenerator` 재사용 |
| `expiresAt` | timestamp | 발급 + 10분 |
| `consumedAt` | timestamp, nullable | |

## Provider 설정 (Spring Security `oauth2-client`, Q1:B)

`spring.security.oauth2.client.registration.{kakao,naver,google}`, `.provider.{kakao,naver}`(google은 내장 `CommonOAuth2Provider`)로 등록. client-id/secret은 환경변수(현재는 값 없이 구조만, 사전 결정 사항).

## 다른 Unit과의 계약 (Code Generation 단계에서 추가될 예정)

- `AccountService.findByEmail(String)` — 동일 이메일 기존 계정 존재 여부 확인용 (신규 메서드)
- `AccountService.createSociallyVerifiedAccount(String email, String randomPasswordHash)` — provider가 이미 이메일 소유를 확인했으므로 이메일 인증 절차 없이 즉시 `ACTIVE` 계정 생성 (신규 메서드)
- `AccountService.verifyPassword(UUID accountId, String rawPassword)` — 연동 확인 시 기존 계정 비밀번호 검증용 (신규 메서드)
- `TokenIssuanceService.issue(UUID accountId, Role role)` — 로그인/연동 성공 시 토큰 발급 (기존 메서드, Token Unit)
