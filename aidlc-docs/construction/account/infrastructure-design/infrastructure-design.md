# Infrastructure Design — Unit: Account

공통 결정(배포 환경, 데이터 스토어, RabbitMQ, 모니터링)은 `aidlc-docs/construction/shared-infrastructure.md` 참고. 이 문서는 Account Unit 고유 매핑만 다룬다.

## 논리 컴포넌트 → 인프라 매핑

| 논리 컴포넌트 | 인프라 매핑 |
|---|---|
| `AccountRepository`, `EmailVerificationTokenRepository`, `PasswordResetTokenRepository` | MariaDB (공유 인스턴스, `account`/`email_verification_token`/`password_reset_token` 테이블) |
| `EmailEventPublisher` | RabbitMQ (공유 인스턴스, `email.events` topic exchange) |
| `OpaqueTokenGenerator` | 인프라 자원 아님 — `com.ecommerce.auth.shared` 인프로세스 유틸리티 |
| `PasswordPolicy`, `AccountService` | 인프라 자원 아님 — 애플리케이션 로직 |

## 로컬 개발 환경 설정 값 (초안)

| 항목 | 값 |
|---|---|
| RabbitMQ 연결 | `amqp://rabbitmq:5672` (Docker Compose 네트워크 내부 호스트명) |
| RabbitMQ 자격증명 | 환경변수 `RABBITMQ_USERNAME`/`RABBITMQ_PASSWORD` (로컬 기본값은 `.env.example` 참고) |
| 이메일 인증 토큰 TTL | 24시간 (설정값으로 노출) |
| 비밀번호 재설정 토큰 TTL | 30분 (설정값으로 노출) |
