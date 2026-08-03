# NFR Requirements Plan — Unit: RateLimit

Token/Account/SocialLogin Unit에서 확정된 공통 사항(수평 확장성, PBT=jqwik, 표준 커넥션 풀)을 상속한다.

## Execution Checklist

- [x] Step A: Resolve question below (gate)
- [x] Step B: Generate `nfr-requirements.md` (Security/PBT 준수 표 포함)
- [x] Step C: Generate `tech-stack-decisions.md`

## Question (GATE)

### Question 1: Redis 장애 시 동작 — Fail-Open vs Fail-Closed
Token Unit은 (인증 검증이라는 보안 핵심 기능이라) Redis 장애 시 fail-closed(거부)를 택했습니다. RateLimit은 성격이 다릅니다 — 이 기능 자체가 남용 "방지"이지 인증의 필수 전제가 아닙니다. Redis가 죽었을 때 signup/login 자체를 막는 것(fail-closed)이 맞을까요, 아니면 속도 제한 없이 요청을 통과시키는 것(fail-open)이 맞을까요?

A) **Fail-open** — Redis 장애 시 Rate Limit/차단 검사를 건너뛰고 요청을 통과시킨다. Redis 장애가 곧 "회원가입/로그인 전면 중단"으로 이어지는 것을 방지(가용성 우선). 장애 중에는 일시적으로 남용 방지 기능이 없다는 리스크를 감수.

B) **Fail-closed** — Token Unit과 동일하게 거부. SECURITY-15(fail-safe defaults)를 엄격히 따름. 단, Redis 장애가 회원가입/로그인 전체를 막는 부작용이 있음(가용성 저하).

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## 사전 결정 사항 (질문 없이 적용)

- **성능**: 요청당 Redis 왕복 1~2회 추가 — p99 100ms 목표에 미미한 영향, 별도 목표 조정 없음.
- **PBT 프레임워크**: jqwik 상속 (PBT-09).
