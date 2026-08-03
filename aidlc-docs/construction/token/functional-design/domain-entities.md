# Domain Entities — Unit: Token

## RefreshToken (MariaDB, 영구 저장 — Q5:A)

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | UUID/PK | Refresh Token 레코드 식별자 |
| `accountId` | FK | 소유 계정 |
| `tokenHash` | string | Refresh Token 원문의 해시값 (원문은 저장하지 않음 — SECURITY-03) |
| `familyId` | UUID | 토큰 패밀리 식별자 — 최초 로그인 시 생성, 이후 회전마다 상속됨 |
| `status` | enum | `ACTIVE` \| `ROTATED`(회전으로 대체됨) \| `REVOKED`(로그아웃/재사용탐지/비밀번호변경으로 무효화) |
| `issuedAt` | timestamp | 발급 시각 |
| `expiresAt` | timestamp | 만료 시각 (발급 + 14일, Q1:A) |
| `previousTokenId` | FK (nullable) | 이 토큰이 어떤 토큰을 회전(대체)해서 발급됐는지 (패밀리 체인 추적) |

**보관 정책 (Q5:A)**: `ROTATED`/`REVOKED`/만료된 레코드도 삭제하지 않고 영구 보관한다. 별도 정리 배치는 이 Unit 범위 밖.

## AccessToken (JWT, 무상태 — MariaDB에 저장하지 않음)

발급 시 아래 클레임을 포함해 서명한다 (Q2:A, HMAC HS256):

| 클레임 | 설명 |
|---|---|
| `sub` | accountId |
| `role` | `USER` \| `SELLER` \| `ADMIN` |
| `familyId` | 소속 Refresh Token 패밀리 (블랙리스트 조회 보조용, 선택적) |
| `jti` | Access Token 고유 ID (블랙리스트 키로 사용) |
| `iat` | 발급 시각 |
| `exp` | 만료 시각 (발급 + 15분, Q1:A) |

## AccessTokenBlacklist (Redis, NFR-02)

| 키 | 값 | TTL |
|---|---|---|
| `blacklist:accessToken:{jti}` | `"revoked"` (플래그) | Access Token 남은 만료 시간까지 (그 이후엔 어차피 자체 만료로 거부되므로 TTL 이후 삭제되어도 무방) |

## 외부 검증 계약 (FR-07, Q2:A + Q4:A의 결과)

외부(별도 API 게이트웨이, 다른 백엔드 서비스)는 다음 중 하나로 토큰을 검증한다:
1. **로컬 검증**: 공유된 HMAC 비밀키로 JWT 서명을 직접 검증 (네트워크 호출 없음, 단 블랙리스트 반영은 안 됨 — 로그아웃 직후 즉시성 보장 안 됨)
2. **원격 검증**: `TokenComponent.validate()` API 호출 — 서명, 만료, 블랙리스트를 모두 반영한 정확한 결과. 별도 인증 없이 내부망에서만 호출 가능 전제 (Q4:A)

로그아웃 즉시 무효화(US-304)가 필요한 호출자는 반드시 (2) 원격 검증을 사용해야 한다.
