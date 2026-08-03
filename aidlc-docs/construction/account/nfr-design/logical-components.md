# Logical Components — Unit: Account

| 논리적 컴포넌트 | 역할 | 비고 |
|---|---|---|
| `AccountRepository` (Spring Data JPA) | `Account` CRUD, email 조회 | MariaDB |
| `EmailVerificationTokenRepository` (Spring Data JPA) | 이메일 인증 토큰 CRUD | MariaDB |
| `PasswordResetTokenRepository` (Spring Data JPA) | 비밀번호 재설정 토큰 CRUD | MariaDB |
| `OpaqueTokenGenerator` (신설, `com.ecommerce.auth.shared`) | 무작위 토큰 원문 생성 + 해시 | Token Unit과 공유 (Q1:A) |
| `PasswordPolicy` | 비밀번호 정책 검증(길이/복잡도) | 순수 함수, PBT 대상 후보 |
| `EmailEventPublisher` | RabbitMQ로 이메일 이벤트 발행, 실패 시 non-blocking | Spring AMQP |
| `AccountService` | 5개 절차 오케스트레이션(가입/인증/로그인/재설정요청/재설정실행) | Token Unit의 `TokenIssuanceService` 호출 |

## 큐/캐시 등 인프라 요소

- **큐**: RabbitMQ (신규, `shared-infrastructure.md` 갱신 필요 — Infrastructure Design에서 반영)
- **캐시**: 없음
- **서킷 브레이커**: 없음
