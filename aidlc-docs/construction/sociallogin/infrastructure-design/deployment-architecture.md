# Deployment Architecture — Unit: SocialLogin

별도 배포 단위가 아니다 — `auth-service` 단일 애플리케이션의 일부다. `docker-compose.yml` 변경 없음(새 컨테이너 불필요). Code Generation 단계에서 `application.properties`/`.env.example`에 provider 자격증명·리다이렉트 URL 플레이스홀더만 추가한다.
