# NFR Requirements — Unit: RateLimit

Token/Account/SocialLogin Unit에서 확정된 공통 사항(수평 확장성, PBT=jqwik, 표준 커넥션 풀)을 상속한다.

## Availability / Reliability

- **Fail-open (Q1:A)**: Redis 장애 시 모든 Rate Limit/차단 검사를 건너뛰고 요청을 통과시킨다. Token Unit의 fail-closed 원칙과 의도적으로 다르다 — RateLimit은 인증의 필수 전제가 아닌 남용 방지 기능이므로, 가용성(회원가입/로그인 자체가 막히지 않는 것)을 우선한다. Redis 장애 중에는 남용 방지 기능이 일시적으로 없다는 리스크를 명시적으로 수용한다.
- 재시도/서킷브레이커 없음 — Token Unit의 "단발 재시도" 패턴도 적용하지 않는다(fail-open이므로 재시도로 지연을 늘릴 이유가 약함 — 실패 시 즉시 통과).

## Performance

- 요청당 Redis 왕복 1~2회 추가 — p99 100ms 목표에 미미한 영향, 별도 조정 없음.

## Security Compliance (Security Baseline — 전체 강제)

| Rule | 상태 | 비고 |
|---|---|---|
| SECURITY-01 | Compliant | Redis 암호화 요구사항 상속 |
| SECURITY-03 | Compliant | IP/이메일 등 필요한 식별자만 로깅, 비밀번호 등 민감정보 없음 |
| SECURITY-11 | Compliant | Rate Limit 로직은 별도 컴포넌트로 격리, 이 Unit 자체가 SECURITY-11의 "공개 엔드포인트 rate limiting 요구"를 충족하는 수단 |
| SECURITY-14 | Compliant | 차단 이벤트 감사 로그 요구사항 문서화(functional-design) |
| **SECURITY-15 (명시적 예외)** | Compliant | 기본 원칙은 fail-closed이나, 이 Unit은 인가/보안 핵심 기능이 아니므로 fail-open으로 명시적 예외 처리(Q1:A, 근거 documented) — Account/Token Unit의 fail-closed와 의도적으로 다른 정책임을 분명히 기록 |
| 나머지 | N/A 또는 상속 | 이전 Unit과 동일 근거 |

**Blocking findings**: 없음.

## PBT Compliance

| Rule | 상태 | 비고 |
|---|---|---|
| PBT-09 | Compliant | jqwik 상속 |
| PBT-02, 03, 07, 08 | N/A (이 단계 대상 아님) | Code Generation 단계 |
