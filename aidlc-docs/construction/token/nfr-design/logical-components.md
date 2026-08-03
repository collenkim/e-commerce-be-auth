# Logical Components — Unit: Token

| 논리적 컴포넌트 | 역할 | 비고 |
|---|---|---|
| `JwtSigner`/`JwtParser` (jjwt 래퍼) | Access Token 서명/파싱 | HS256, 비밀키는 시크릿 매니저/환경변수 주입 |
| `RefreshTokenRepository` (Spring Data JPA) | `RefreshToken` 엔티티 CRUD | MariaDB, HikariCP 기본 풀 |
| `TokenBlacklistStore` (Spring Data Redis 래퍼) | jti 블랙리스트 등록/조회 | 단발 재시도 후 fail-closed, TTL = Access Token 잔여 만료시간 |
| `JwtAuthenticationFilter` (`OncePerRequestFilter`) | 매 요청마다 Access Token 검증, Spring Security 컨텍스트에 인증 정보 설정 | 검증 실패 시 401, 체인 중단 |
| `TokenIssuanceService` | 발급/회전/재사용탐지/로그아웃/검증 오케스트레이션 (`business-logic-model.md`의 6개 절차) | `RefreshTokenRepository` + `TokenBlacklistStore` + `JwtSigner`/`JwtParser` 조합 |

## 캐시/큐 등 인프라 요소

- **큐**: 없음 (Token Unit은 메시지 큐를 사용하지 않음 — 이메일 발송은 Account Unit 책임)
- **캐시**: Redis가 곧 블랙리스트이자 유일한 캐시 성격 저장소 (별도 캐시 계층 없음)
- **서킷 브레이커**: 없음 (NFR Design Pattern 결정에 따름)
