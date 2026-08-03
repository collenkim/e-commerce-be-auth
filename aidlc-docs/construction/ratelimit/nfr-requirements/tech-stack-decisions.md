# Tech Stack Decisions — Unit: RateLimit

| 영역 | 선택 | 근거 |
|---|---|---|
| 카운터/차단 저장소 | Redis (`StringRedisTemplate`, Token Unit과 동일 인스턴스) | NFR-02 상속, 별도 저장소 불필요 |
| 카운팅 알고리즘 | 고정 윈도우 (`INCR`+`EXPIRE`) | Functional Design Q3:A |
| PBT 프레임워크 | jqwik | 상속 |

## Shared Infrastructure 갱신 필요 사항

- 없음 — 기존 Redis 인스턴스 재사용, 키 네임스페이스(`ratelimit:*`, `block:*`, `bruteforce:*`)만 추가.
