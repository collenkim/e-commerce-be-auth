# NFR Design Plan — Unit: Token

**입력**: `construction/token/nfr-requirements/*` (fail-closed on Redis 장애, p99<=100ms 목표, jjwt/HS256)

## Execution Checklist

- [x] Step A: Resolve question below (gate)
- [x] Step B: Generate `nfr-design-patterns.md`
- [x] Step C: Generate `logical-components.md`

## Question (GATE)

### Question 1: Redis 장애 시 재시도 정책
NFR Requirements에서 Redis 장애 시 fail-closed(거부)로 이미 결정했습니다. 거부하기 전에 짧게 재시도할까요? (프로젝트 전역 방침상 무거운 회복탄력성 패턴은 지양 — `aidlc-state.md` Resiliency 메모 참고)

A) 재시도 없이 즉시 fail-closed — 가장 단순, Redis 순간 장애도 그대로 거부로 이어짐

B) 짧은 단발성 재시도(예: 1회, 수십 ms 타임아웃) 후에도 실패하면 fail-closed — Resilience4j 등 별도 라이브러리 없이 스프링/Lettuce 기본 재시도 설정으로 충분히 구현 가능, 순간적인 네트워크 blip에 대한 완충

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## 사전 결정 사항 (질문 없이 표준 패턴 적용)

- **확장성 패턴**: 무상태(stateless) 애플리케이션 서버 + 공유 MariaDB/Redis — 별도 샤딩/파티셔닝 설계 없음 (현재 규모에 과함)
- **성능 패턴**: Spring Boot 기본 커넥션 풀(HikariCP for MariaDB, Lettuce 기본 풀 for Redis) 사용 — 별도 튜닝 없이 기본값으로 시작
- **보안 패턴**: Spring Security 필터 체인에 커스텀 JWT 인증 필터(`OncePerRequestFilter`)를 추가해 매 요청마다 토큰 검증 수행 — 기존 스켈레톤의 `spring-boot-starter-security` 의존성과 자연스럽게 통합
- **회복탄력성 패턴**: 서킷 브레이커 등 별도 라이브러리(Resilience4j 등) 도입 안 함 — Resiliency Baseline이 정보성만 적용되는 프로젝트 방침과 일치, 규모에 비해 과한 복잡도
