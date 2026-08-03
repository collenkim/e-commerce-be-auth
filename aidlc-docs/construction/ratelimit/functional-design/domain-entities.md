# Domain Entities — Unit: RateLimit

MariaDB 엔티티 없음 — 모든 상태는 Redis에 있다(NFR-02/03 상속). 아래는 JPA 엔티티 대신 **Redis 키 스키마**다.

| 키 패턴 | 값 | TTL | 용도 |
|---|---|---|---|
| `ratelimit:ip:{endpoint}:{ip}` | 카운터(정수) | 60초 | IP 기준 분당 요청 수 (US-401) |
| `ratelimit:ip-violation:{ip}` | 카운터(정수) | 60초 | 최근 1분 내 429 발생 횟수 — 임계치 도달 시 IP 차단 트리거 (US-403) |
| `ratelimit:account:{normalizedEmail}` | 카운터(정수) | 10분 | 계정 기준 로그인 실패 횟수 (US-402) |
| `block:ip:{ip}` | 차단 플래그 + 사유(`RATE_LIMIT`\|`BRUTE_FORCE`) | 15분 | IP 차단 상태 (US-403/404), TTL 만료로 자동 해제 (US-405) |
| `block:account:{normalizedEmail}` | 차단 플래그 | 15분 | 계정 로그인 일시 제한 (US-402), TTL 만료로 자동 해제 |
| `bruteforce:ip-targets:{ip}` | Set<정규화된 이메일> | 5분 | 해당 IP가 최근 5분간 시도한 서로 다른 계정 목록 — 카디널리티가 임계치 도달 시 브루트포스로 간주 (US-404) |

**엔드포인트 범위 (Q1:A)**: `/api/auth/signup`, `/api/auth/login`만 적용. (다른 엔드포인트 확장 여부는 `integration-points.md`에 남은 항목 중 이번 결정으로 해소 — B안은 채택하지 않음)

**임계치 (Q2:A)**:

| 규칙 | 임계치 | 조치 |
|---|---|---|
| IP 기준 요청 수(US-401) | 분당 10회 초과 | 429 |
| 계정 기준 로그인 실패(US-402) | 10분 내 5회 | 해당 계정 로그인 15분 제한 |
| IP 자동 차단(US-403) | 1분 내 429를 3번 받음 | 해당 IP 15분 차단 |
| 브루트포스 IP 차단(US-404) | 5분 내 서로 다른 계정 10개 이상 로그인 실패 시도 | 해당 IP 즉시 15분 차단 |

**알고리즘 (Q3:A)**: 고정 윈도우(Redis `INCR`+`EXPIRE`). 경계 버스트 현상은 알려진 트레이드오프로 수용(단순성 우선).
