# Performance Test Instructions

## Scope Note (read first)

This project's Resiliency Baseline was explicitly downgraded to informational-only during Requirements Analysis ("인프라 장애 대응은 너무 과하게 하지 말았으면해") and no unit set a hard, measured performance target — each NFR Requirements stage recorded a **standard default** (p99 ≤ 100ms per request, except SocialLogin which explicitly has no target due to external OAuth2 provider latency) with the explicit caveat "실측 트래픽 확보 후 재조정" (no real traffic exists yet to calibrate against). Formal load/stress testing was **not** requested or designed for in this phase. This document is intentionally lightweight — expand it if/when real usage data or an actual performance requirement emerges.

## Performance Targets (as documented per-unit, unmeasured)
- **General target**: p99 ≤ 100ms per request, single instance, informal (Token/Account/RateLimit/Authorization Units).
- **Exception**: SocialLogin — no target; latency is dominated by external provider (Kakao/Naver/Google) round trips, outside this service's control.
- **Throughput / Concurrent Users / Error Rate**: Not specified anywhere in `requirements.md` or any unit's NFR Requirements — no target exists to test against.

## If/When Formal Performance Testing Is Needed

### 1. Prepare Test Environment
```bash
docker-compose up --build
```
Run against the full stack (real MariaDB/Redis/RabbitMQ), not H2/mocks — the fixed-window Redis rate limiter and JWT signature verification are the most latency-relevant paths.

### 2. Suggested Tooling
No load-testing tool is currently part of this project. A lightweight option: [k6](https://k6.io/) or `wrk` against `/api/auth/login` (the most representative hot path: RateLimit filter → password verification (BCrypt, intentionally slow) → JWT issuance).

### 3. What to Watch
- **BCrypt cost factor 12** (Account Unit decision) is a deliberate, measurable source of latency on every login/signup — expect tens of milliseconds from hashing alone. If p99 100ms is ever violated, check this first before assuming a bug.
- **Redis round trips**: `validate()` (Token) and rate-limit checks each add 1 Redis call; login adds up to 3 (IP limit, account block check, failure record).
- **RabbitMQ publish**: fire-and-forget with a broad exception catch (non-blocking) — should never add meaningful latency to the request path, but worth confirming under load that the broker connection isn't a bottleneck.

## Status
**Not executed.** No performance requirement was defined precisely enough to test against, and no load-testing tooling exists in this project yet. Marked N/A in the overall Build and Test summary rather than fabricating results.
