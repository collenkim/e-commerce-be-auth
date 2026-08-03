# NFR Requirements Plan — Unit: Token

**입력**: `construction/token/functional-design/*`, `requirements.md`(NFR-01~07), Extension Configuration(Security Baseline: 전체 강제, Resiliency: 정보성만/추가 질문 금지, PBT: Partial — PBT-02,03,07,08,09만 강제)

## Execution Checklist

- [x] Step A: Resolve questions below (gate)
- [x] Step B: Generate `nfr-requirements.md` (Security/PBT 준수 표 포함)
- [x] Step C: Generate `tech-stack-decisions.md`

## Questions (GATE)

### Question 1: JWT 라이브러리
NFR-07에서 `jjwt` 또는 Spring Security OAuth2 Resource Server 둘 다 후보로 언급되었습니다. Token Unit에서 무엇을 사용할까요?

A) `jjwt` (io.jsonwebtoken) — 발급과 검증을 모두 직접 제어, HS256 대칭키 발급/검증에 가장 단순하고 널리 쓰임

B) Spring Security `oauth2-resource-server` (Nimbus JOSE 기반) — Spring Security 필터 체인과 통합된 검증에 강점, 다만 발급 로직은 별도로 구현 필요 (Resource Server는 주로 검증용)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: 예상 트래픽/성능 목표
그린필드라 실제 트래픽 데이터가 없습니다. Token Unit(발급/갱신/검증)에 대한 성능 목표를 어느 수준으로 잡을까요?

A) 특별한 목표 없음 — 표준적인 응답 시간(예: p99 100ms 이하, 단일 인스턴스 기준)을 목표로 하되 실제 트래픽 관찰 후 재조정. 별도 부하 테스트/용량 계획은 이번 단계에서 설계하지 않음.

B) 구체적 목표 지정 — [Answer]: 뒤에 목표 수치(RPS, 지연시간 등)를 적어주세요

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## 사전 결정 사항 (질문 없이 표준 관행 적용, 완료 메시지에서 확인 가능)

- **PBT 프레임워크 (PBT-09)**: `jqwik` — JUnit 5 통합, Java 표준 스택과 자연스럽게 맞음 (property-based-testing.md 권장 표 기준)
- **Redis 장애 시 검증 동작**: Security Baseline SECURITY-15(Fail-safe Defaults)에 따라 fail-closed — 블랙리스트 조회가 실패하면 유효하지 않은 것으로 간주해 거부 (가용성보다 보안 우선)
- **비밀번호/토큰 원문 로깅 금지**: SECURITY-03에 따라 Access/Refresh Token 원문은 어떤 로그에도 남기지 않음 (해시/jti만 로깅 가능)
