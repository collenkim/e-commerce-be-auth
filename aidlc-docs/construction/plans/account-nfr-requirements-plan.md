# NFR Requirements Plan — Unit: Account

**참고**: Token Unit NFR Requirements(`construction/token/nfr-requirements/`)에서 이미 확정된 프로젝트 공통 사항은 재질문하지 않고 그대로 상속한다 — PBT 프레임워크(jqwik), Security Baseline 준수 방식, 표준 커넥션 풀, p99 100ms 기본 목표.

## Execution Checklist

- [x] Step A: Resolve question below (gate)
- [x] Step B: Generate `nfr-requirements.md` (Security/PBT 준수 표 포함)
- [x] Step C: Generate `tech-stack-decisions.md`

## Question (GATE)

### Question 1: 이메일 발송 이벤트용 메시지 큐
Application Design(Q5:B)에서 "메시지 큐에 이벤트만 발행, consumer 미구현"으로 결정했지만 구체적인 브로커는 미정입니다. 로컬 개발 환경(Token Unit Infrastructure Design Q1:A) 기준으로 무엇을 사용할까요?

A) RabbitMQ — Docker Compose에 컨테이너 추가, Spring AMQP로 발행. 향후 실제 Notification 서비스가 consumer로 붙기 쉬운 범용 브로커.

B) 브로커 없이 애플리케이션 내부 로그/스텁으로 발행부만 흉내 — 실제 메시지 브로커 도입은 향후(Notification 서비스 도입 시점)로 완전히 미룸. 가장 단순하지만 "발행 채널 마련"이라는 Application Design 결정의 의도와는 약간 거리가 있음.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## 사전 결정 사항 (질문 없이 적용)

- **BCrypt 강도**: cost factor 12 (OWASP 권장 범위, 로그인 지연에 큰 영향 없는 수준)
- **이벤트 발행 실패 시 처리**: 회원가입/재설정 요청 자체는 실패시키지 않는다 — 이메일 발송은 핵심 트랜잭션이 아니므로 발행 실패는 로깅만 하고 계정 생성/재설정 토큰 발급은 성공 처리한다(SECURITY-15의 "fail closed"는 인가/보안 결정에 적용되는 것이지, 알림성 부가 채널에는 적용하지 않음).
