# Infrastructure Design Plan — Unit: Token

**참고**: 클라우드 제공자/배포 환경 같은 결정은 5개 Unit이 전부 하나의 배포 단위(`auth-service`)를 공유하므로, 여기서 확정되는 항목은 `aidlc-docs/construction/shared-infrastructure.md`로 분리해 이후 Unit(Account, SocialLogin, RateLimit, Authorization)들이 재사용한다. Token 고유 사항만 이 Unit 문서에 남긴다.

## Execution Checklist

- [x] Step A: Resolve questions below (gate)
- [x] Step B: Generate `infrastructure-design.md` (Token 고유)
- [x] Step C: Generate `deployment-architecture.md` (Token 고유)
- [x] Step D: Generate `shared-infrastructure.md` (5개 Unit 공유 결정)

## Questions (GATE)

### Question 1: 배포 대상 환경
지금까지 요구사항/설계 어디에도 실제 배포 대상(클라우드/온프레미스)이 정해지지 않았습니다.

A) 지금은 로컬 개발 환경만 구성 — Docker Compose로 앱 + MariaDB + Redis를 띄우는 수준까지만 설계. 실제 클라우드 배포 대상(AWS/GCP/Azure 등)은 아직 정하지 않고, 이후 별도로 결정.

B) 특정 클라우드 대상이 이미 정해짐 — [Answer]: 뒤에 제공자와 대략적인 서비스(예: AWS ECS+RDS+ElastiCache)를 적어주세요.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: 모니터링/로깅 도구
NFR Requirements(SECURITY-03/14)에서 구조화 로깅과 인증 실패 알림 요구사항을 문서화했습니다. 지금 단계에서 구체적인 도구까지 정할까요?

A) 아직 정하지 않음 — 로컬 개발 단계에서는 Spring Boot 기본 로깅(콘솔)만 사용하고, 실제 모니터링 스택(예: ELK, CloudWatch, Prometheus/Grafana)은 클라우드 배포 대상이 정해질 때 결정

B) 지금 정함 — [Answer]: 뒤에 원하는 도구를 적어주세요

[Answer]: A

## 사전 결정 사항 (질문 없이 적용)

- **메시징 인프라**: Token Unit은 메시지 큐를 사용하지 않음 (N/A) — Account Unit이 이메일 이벤트 발행 큐를 사용하며, 그 인프라는 Account Unit의 Infrastructure Design에서 다룬다.
- **네트워킹**: API 게이트웨이는 별도 프로젝트라 이 서비스 범위 밖. 이 서비스 앞단의 로드밸런서/인그레스는 Q1 답변에 따라 결정(로컬 단계면 N/A).
- **공유 인프라 전략**: MariaDB, Redis는 5개 Unit이 공유하는 단일 인스턴스(들)로, `shared-infrastructure.md`에 한 번만 정의하고 이후 Unit들은 참조만 한다.
