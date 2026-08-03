# Deployment Architecture — Unit: Account

Account Unit도 Token Unit과 마찬가지로 별도 배포 단위가 아니다 — `auth-service` 단일 애플리케이션의 일부다.

## 로컬 개발 배포 (Q1:A 상속)

```
docker-compose.yml
├── auth-service   (이 저장소 빌드 결과물, 5개 Unit 전부 포함)
├── mariadb        (공유)
├── redis          (공유)
└── rabbitmq       (신규 — Account Unit이 이메일 이벤트 발행에 사용)
```

`docker-compose.yml`, `shared-infrastructure.md`를 이 Unit의 Code Generation 단계에서 갱신한다.
