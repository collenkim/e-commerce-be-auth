# Tech Stack Decisions — Unit: Account

| 영역 | 선택 | 근거 |
|---|---|---|
| 비밀번호 해싱 | Spring Security `BCryptPasswordEncoder` (cost factor 12) | 기존 `spring-boot-starter-security` 의존성과 자연스럽게 통합, FR-01 |
| 메시지 브로커 | RabbitMQ (Spring AMQP) | Q1:A — 로컬 Docker Compose에 컨테이너 추가, 향후 Notification 서비스 consumer 연결 용이 |
| 영속성 | Spring Data JPA + MariaDB | Token Unit과 동일(NFR-01 상속) |
| PBT 프레임워크 | jqwik | Token Unit에서 이미 선택, 그대로 상속 (PBT-09) |

## Shared Infrastructure 갱신 필요 사항

- `shared-infrastructure.md`에 RabbitMQ 컨테이너 추가 필요 (Infrastructure Design 단계에서 반영).
