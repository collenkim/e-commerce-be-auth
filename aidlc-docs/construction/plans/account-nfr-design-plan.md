# NFR Design Plan — Unit: Account

## Execution Checklist

- [x] Step A: Resolve question below (gate)
- [x] Step B: Generate `nfr-design-patterns.md`
- [x] Step C: Generate `logical-components.md`

## Question (GATE)

### Question 1: 불투명 토큰(인증/재설정) 생성·해시 로직 재사용
Token Unit의 `TokenIssuanceService`가 Refresh Token용으로 이미 "무작위 원문 생성 + SHA-256 해시" 로직을 구현했습니다. Account Unit의 이메일 인증/비밀번호 재설정 토큰도 동일한 방식(무작위 원문 + 해시 저장)이 필요합니다. 어떻게 할까요?

A) 공용 유틸리티로 추출 — `com.ecommerce.auth.shared`에 `OpaqueTokenGenerator`(원문 생성 + 해시)를 만들고 Token Unit도 리팩터링해 재사용, Account Unit도 사용 (중복 제거)

B) Account Unit에서 별도로 동일 로직을 다시 구현 — Unit 간 결합을 최소화(각 Unit이 완전히 독립적으로 완결), 코드는 일부 중복

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## 사전 결정 사항 (질문 없이 적용, Token Unit 패턴 상속)

- **확장성/성능 패턴**: 무상태 애플리케이션, 기본 커넥션 풀 — Token Unit과 동일
- **RabbitMQ 발행 실패 처리**: try-catch로 감싸 실패 시 로깅만 하고 예외를 전파하지 않음(계정 생성/재설정 자체는 성공) — Resilience4j 등 별도 라이브러리 도입 안 함(NFR Requirements Q1 연장)
- **보안 패턴**: Account Unit의 엔드포인트(가입/인증/로그인/재설정)는 모두 공개 엔드포인트이므로 Token Unit의 `JwtAuthenticationFilter`와 상호작용하지 않는다
