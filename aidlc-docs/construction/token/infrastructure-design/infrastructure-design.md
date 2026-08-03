# Infrastructure Design — Unit: Token

공통 결정(배포 환경, 데이터 스토어, 모니터링, 네트워킹)은 `aidlc-docs/construction/shared-infrastructure.md` 참고. 이 문서는 Token Unit 고유 매핑만 다룬다.

## 논리 컴포넌트 → 인프라 매핑

| 논리 컴포넌트 (`nfr-design/logical-components.md`) | 인프라 매핑 |
|---|---|
| `RefreshTokenRepository` | MariaDB (공유 인스턴스, `refresh_token` 테이블) — Spring Data JPA + HikariCP |
| `TokenBlacklistStore` | Redis (공유 인스턴스, 키 네임스페이스 `blacklist:accessToken:{jti}`) — Lettuce 클라이언트, 단발 재시도 설정 |
| `JwtSigner`/`JwtParser` | 인프라 자원 아님 — 인프로세스 라이브러리(jjwt). 비밀키만 외부 자원: 환경변수(로컬) → 향후 클라우드 결정 시 시크릿 매니저로 이관 |
| `JwtAuthenticationFilter` | 인프라 자원 아님 — 애플리케이션 내 Spring Security 필터 |
| `TokenIssuanceService` | 인프라 자원 아님 — 애플리케이션 서비스 계층 |

## 로컬 개발 환경 설정 값 (초안)

| 항목 | 값 |
|---|---|
| MariaDB 연결 | `jdbc:mariadb://mariadb:3306/auth_service` (Docker Compose 네트워크 내부 호스트명) |
| Redis 연결 | `redis://redis:6379` |
| JWT 비밀키 | 환경변수 `JWT_HMAC_SECRET` (로컬 `.env`, 저장소에 커밋 금지 — SECURITY-12) |
| Access Token TTL | 15분 (설정값으로 노출, 하드코딩 금지) |
| Refresh Token TTL | 14일 |
