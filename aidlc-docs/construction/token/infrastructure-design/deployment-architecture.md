# Deployment Architecture — Unit: Token

Token Unit은 별도 배포 단위가 아니다 — `auth-service` 단일 애플리케이션의 일부로 다른 4개 Unit과 함께 배포된다 (`unit-of-work.md` 참고).

## 로컬 개발 배포 (현재 단계, Q1:A)

```
docker-compose.yml
├── auth-service   (이 저장소 빌드 결과물, 5개 Unit 전부 포함)
├── mariadb        (공유, Token + Account 등이 사용)
└── redis          (공유, Token 블랙리스트 + RateLimit 카운터)
```

- `auth-service` 컨테이너는 MariaDB/Redis 컨테이너에 Docker Compose 내부 네트워크로 접속한다.
- 클라우드 배포 대상이 정해지기 전까지는 이 구성이 유일한 배포 아키텍처다.

## 향후 (클라우드 대상 결정 시 갱신 필요)

- `auth-service` → 컨테이너 오케스트레이션 서비스(예: ECS/Cloud Run/AKS 등, 미정)
- `mariadb` → 관리형 RDB 서비스로 이관 (미정)
- `redis` → 관리형 캐시 서비스로 이관 (미정)

이 섹션은 Q1 답변이 바뀌면(클라우드 대상 확정) `shared-infrastructure.md`와 함께 갱신한다.
