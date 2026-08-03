# NFR Design Patterns — Unit: Token

## Resilience Pattern: Redis 접근 — 단발 재시도 후 Fail-Closed (Q1:B)

- `validate()`(및 로그아웃 시 블랙리스트 등록)에서 Redis 호출이 실패하면 **1회, 수십 ms 타임아웃**으로 즉시 재시도한다.
- 재시도까지 실패하면 fail-closed(요청 거부)로 처리한다 (NFR Requirements에서 확정된 방침).
- 별도 서킷 브레이커/백오프 라이브러리(Resilience4j 등)는 도입하지 않는다 — Lettuce 클라이언트 자체의 커맨드 타임아웃 설정 + 애플리케이션 레벨의 단순 재시도(예: try 1회 실패 시 즉시 1회 더) 조합으로 충분.
- MariaDB 접근 실패(발급/회전 시 저장 불가)는 재시도 없이 즉시 요청 실패 처리 — 재시도로 지연을 늘리기보다 클라이언트가 재요청하도록 유도 (쓰기 경로는 상태 일관성이 더 중요).

## Scalability Pattern

- 무상태 애플리케이션: Access/Refresh Token 상태는 전부 MariaDB/Redis에 있고 인스턴스 로컬 메모리에 아무것도 캐시하지 않는다 → 인스턴스를 수평으로 늘려도 정합성 유지 (NFR-03).
- 샤딩/파티셔닝은 설계하지 않는다 (현재 규모에 불필요).

## Performance Pattern

- MariaDB: Spring Boot 기본 HikariCP 커넥션 풀, 기본 설정값으로 시작.
- Redis: Lettuce 기본 커넥션 풀, 기본 설정값으로 시작.
- `validate()` 경로에는 DB 접근이 없다 (Redis 조회 + 로컬 JWT 서명 검증만) — p99 100ms 목표 달성에 유리한 구조.

## Security Pattern

- Spring Security 필터 체인에 커스텀 `OncePerRequestFilter`(JWT 인증 필터)를 등록해 보호 대상 요청마다 Authorization 헤더의 Access Token을 파싱·검증한다.
- 검증 실패(서명 무효/만료/블랙리스트 등록됨) 시 필터 단계에서 401을 반환하고 이후 체인으로 진행하지 않는다.
- 필터는 jjwt로 서명 검증만 수행하고, 블랙리스트 조회(Redis)는 별도 단계로 분리해 위 Resilience Pattern(재시도+fail-closed)을 적용한다.
