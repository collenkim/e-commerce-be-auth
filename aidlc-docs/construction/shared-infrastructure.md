# Shared Infrastructure (모든 Unit 공통)

5개 Unit(Token, Account, SocialLogin, RateLimit, Authorization)이 하나의 배포 단위(`auth-service`)를 구성하므로, 아래 결정은 Unit별로 반복하지 않고 여기서 한 번만 정의한다. 최초로 이 문서를 만든 Unit: **Token** (2026-07-31, Infrastructure Design Q1/Q2 답변 기준).

## 배포 대상 환경 (Q1:A)

- **현재 단계**: 로컬 개발 환경만 구성한다. 실제 클라우드 배포 대상(AWS/GCP/Azure 등)은 아직 결정하지 않았다.
- **구성**: Docker Compose로 다음 4개 컨테이너를 함께 기동
  - `auth-service` (이 저장소의 Spring Boot 앱, 단일 배포 단위)
  - `mariadb` (MariaDB 컨테이너)
  - `redis` (Redis 컨테이너)
  - `rabbitmq` (RabbitMQ 컨테이너, Account Unit NFR Requirements Q1:A — 이메일 이벤트 발행용)
- 클라우드 배포 대상이 결정되면, 이 문서를 갱신하고 영향받는 Unit들의 Infrastructure Design을 필요한 범위에서 재검토한다 (전면 재작업 아님 — 컨테이너 이미지를 관리형 서비스로 매핑하는 수준).

## 데이터 스토어

| 스토어 | 용도 | 소유 Unit(대표) | 비고 |
|---|---|---|---|
| MariaDB | 계정, RefreshToken, 소셜 계정 연동 등 영속 데이터 | Token(RefreshToken), Account(계정) | 단일 인스턴스(로컬), 스키마는 Unit별 테이블로 분리 |
| Redis | Access Token 블랙리스트, Rate Limit 카운터/IP 차단 | Token(블랙리스트), RateLimit(카운터) | 단일 인스턴스(로컬), 키 네임스페이스로 용도 구분 (`blacklist:*`, `ratelimit:*`) |

## 모니터링/로깅 (Q2:A)

- 현재 단계에서는 Spring Boot 기본 로깅(콘솔 출력)만 사용한다.
- 실제 모니터링 스택(ELK/CloudWatch/Prometheus+Grafana 등)은 클라우드 배포 대상이 정해진 뒤 별도로 결정한다.
- SECURITY-03(구조화 로깅)/SECURITY-14(알림) 요구사항은 유지되며, 도구 선택만 보류된 상태다 — 로컬 단계에서도 로그 포맷은 구조화(JSON 등)를 지향한다.

## 네트워킹

- API 게이트웨이는 별도 프로젝트이며 이 저장소 범위 밖이다.
- 로컬 개발 단계에서는 앱을 호스트에 직접 포트 노출(Docker Compose)하며, 별도 로드밸런서/인그레스를 두지 않는다.

## 메시징

- **RabbitMQ** — 이메일 발송 이벤트(`EmailVerificationRequested`, `PasswordResetRequested`) 발행 전용 (Account Unit NFR Requirements Q1:A). Consumer는 이 프로젝트 범위 밖(향후 Notification 서비스가 연결).
- Exchange: `email.events` (topic), 라우팅 키: `email.verification.requested`, `email.password-reset.requested`. 큐는 consumer가 없으므로 이번 단계에서 생성하지 않는다 — exchange 발행까지만 구현.
