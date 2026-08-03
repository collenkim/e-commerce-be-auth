# Logical Components — Unit: RateLimit

| 논리적 컴포넌트 | 역할 | 비고 |
|---|---|---|
| `RateLimitService` | IP/계정 검사·기록, 차단 판정, fail-open 처리 | Redis 직접 사용, 다른 Unit 호출 안 함 |
| `RateLimitFilter` | `/api/auth/signup`, `/api/auth/login` 진입 시 IP 차단/요청수 검사 | 일반 서블릿 필터, `FilterRegistrationBean`으로 등록(SecurityFilterChain 아님) |
| `RateLimitProperties` | 임계치/TTL 설정값 | 하드코딩 금지 |

## 큐/캐시 등 인프라 요소

- **캐시/카운터 저장소**: Redis (기존 인스턴스 재사용)
- **큐**: 없음
- **서킷 브레이커**: 없음 (fail-open 자체가 회복탄력성 메커니즘)
