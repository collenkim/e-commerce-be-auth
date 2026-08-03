# Infrastructure Design — Unit: RateLimit

공통 결정은 `shared-infrastructure.md` 참고 — 새 컨테이너/데이터스토어 불필요(기존 Redis 재사용).

## 논리 컴포넌트 → 인프라 매핑

| 논리 컴포넌트 | 인프라 매핑 |
|---|---|
| `RateLimitService` | Redis(공유 인스턴스, 키 네임스페이스 `ratelimit:*`/`block:*`/`bruteforce:*`) |
| `RateLimitFilter`, `RateLimitProperties` | 인프라 자원 아님 |

## 설정 값 (초안)

| 항목 | 값 |
|---|---|
| IP 요청 한도 | 60초당 10회 |
| IP 위반 차단 임계치 | 60초당 3회 |
| 계정 실패 한도 | 10분당 5회 |
| 브루트포스 임계치 | 5분 내 서로 다른 계정 10개 |
| 차단 지속 시간 | 15분 |

모두 설정값으로 노출한다(하드코딩 금지).
