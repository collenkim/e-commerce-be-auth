# Requirements: e-commerce-be-auth (인증/인가 서비스)

## Intent Analysis Summary

- **User Request**: "AI-DLC 인증/인가 서비스 만들어줘. API 요청에 Jwt 토큰 사용할꺼고, ratelimit도 적용해줘. 동일한 ip 요청에 대한 block 처리. 그리고 SSO 로그인 소셜 로그인 등 처리도 인증/인가에서 담당하는지."
- **Request Type**: New Project (greenfield service build on top of an existing empty Spring Boot skeleton)
- **Scope Estimate**: System-wide within this service — the service covers authentication, authorization, JWT lifecycle, rate limiting, IP blocking, and social login
- **Complexity Estimate**: Complex — multiple integrated concerns (JWT issuance/rotation, OAuth2 social login across 3 providers, distributed rate limiting/IP blocking, RBAC)

## Architectural Callout (read first)

**Superseded 2026-07-31**: This service is a **pure Authentication/Authorization API** — it does NOT act as the platform's API Gateway. The API Gateway (request routing/reverse-proxying to backend microservices) is being built as a **separate project**, out of scope here (explicit user decision: "게이트웨이는 따로 만들래. 인증/인가 api만 구현해줘."). This service's responsibility ends at issuing JWTs (with role claims) and exposing validation/introspection so the separate gateway and/or individual backend services can verify tokens and enforce role-based access themselves.

## Functional Requirements

| ID | Requirement |
|---|---|
| FR-01 | Users can register and log in with email + password. Passwords are hashed with an adaptive algorithm (BCrypt). |
| FR-02 | On successful login, the service issues a short-lived JWT Access Token and a long-lived Refresh Token. |
| FR-03 | Refresh Tokens support rotation and reuse detection: each refresh exchange issues a new refresh token and invalidates the old one; reuse of an already-rotated token revokes the token family. |
| FR-04 | On logout, the Refresh Token is immediately revoked. The Access Token is added to a blacklist so it is immediately unusable even before its natural expiry. |
| FR-05 | Social login via OAuth2 is supported for **Kakao, Naver, and Google** (Apple and Facebook are explicitly out of scope for this phase — noted as future extension). |
| FR-06 | Role-based access control with three roles: `USER` (buyer), `SELLER`, `ADMIN`. This service's own protected endpoints (e.g., admin user management) enforce role checks server-side; role claims embedded in the issued JWT allow the separately-built API Gateway and other backend services to enforce their own role checks. |
| FR-07 | The service issues JWTs (with embedded role claims) and exposes a token validation/introspection capability so other services can verify a token's validity and role without re-implementing token logic. This service does NOT route or proxy requests to other backend services — that is the responsibility of a separately-built API Gateway (out of scope for this project). |
| FR-08 | Rate limiting applies to authentication endpoints only (login, signup, and related auth flows). Two independent limits apply: per-IP and per-account. |
| FR-09 | An IP is automatically, temporarily blocked (HTTP 429/403) when either (a) it exceeds the general auth-endpoint rate limit, or (b) it produces repeated failed login attempts (brute-force pattern). Blocks expire automatically after a cool-down window; no manual admin block/unblock is required for this phase. |
| FR-10 | Email verification is required after signup before the account is fully active. |
| FR-11 | Users can reset their password via an emailed reset link/token flow. |
| FR-12 | This service's own endpoints default-deny unauthenticated requests, except for the endpoints that are inherently public (signup, login, social login start, token refresh). Public access to other domains' resources (e.g., product catalog browsing) is the responsibility of those services / the separately-built API Gateway, not this service — see Out of Scope. |

## Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-01 | **Data storage**: MariaDB is the system of record for users, roles, and Refresh Tokens (persistence/audit trail). |
| NFR-02 | **Fast-path storage**: Redis holds the Access Token blacklist and all rate-limit / IP-block counters, keyed for O(1) lookup with TTL-based auto-expiry. |
| NFR-03 | **Horizontal scalability**: Rate-limit, IP-block, and token-blacklist state MUST be correct across multiple concurrently running instances of this service (Redis-backed, not in-process memory). |
| NFR-04 | **Security Baseline** (AI-DLC SECURITY-01..15) is enabled and enforced as blocking constraints across all subsequent stages — see Security Compliance sections in later stage completions. |
| NFR-05 | **Resiliency posture** is intentionally minimal for this phase (explicit user direction — do not over-engineer): single-region + multi-AZ deployment, no cross-region disaster recovery, a lightweight (proposed, not pre-existing) change-management process. The Resiliency Baseline extension is **not** enforced as a blocking gate; no further dedicated resiliency design questions (CI/CD/rollback/deployment style, chaos/DR testing, incident response) will be raised — simplest reasonable defaults are used instead. |
| NFR-06 | **Testing**: Property-Based Testing is enabled in **Partial** mode — enforced only for pure functions and serialization round-trips (e.g., JWT claim encode/decode, DTO (de)serialization). Not required for stateful components or broader business logic. |
| NFR-07 | **Tech stack** builds on the existing skeleton: Java 17, Spring Boot, Gradle (Kotlin DSL), Spring Security, Spring Data JPA, MariaDB driver, Lombok. New dependencies to be introduced: JWT library (e.g., `jjwt` or Spring Security OAuth2 Resource Server), Spring Security OAuth2 Client (social login), Redis client (e.g., Spring Data Redis / Lettuce), Bucket4j or Redis-based rate-limiting library. Existing blocking Spring MVC (servlet) stack is retained — no reactive/gateway runtime needed since this service does not proxy requests. |

## User Scenarios (representative)

- A buyer registers with email/password, verifies their email, and logs in to receive an access + refresh token pair.
- A buyer logs in via Kakao; the service creates or links a local account and issues the same token pair as email/password login.
- A client's access token expires; the client calls the refresh endpoint with its refresh token and receives a new token pair; the old refresh token is invalidated.
- A malicious client makes 20 login attempts per minute from one IP; after exceeding the threshold, that IP is auto-blocked for a cool-down period.
- A malicious client repeatedly guesses passwords for one account from a rotating set of IPs; the per-account limit blocks further attempts on that account regardless of source IP.
- A user logs out; their refresh token is revoked and their still-unexpired access token is rejected on the very next request due to the blacklist check.
- A separately-built API Gateway (or another backend service) receives a request with a JWT and calls this service's token validation/introspection capability to confirm the token is valid and read its role claim.

## Business Context

- Target system: e-commerce platform backend, multi-service architecture (this is the first service being built).
- Primary goal: centralize authentication, authorization, and JWT issuance so downstream microservices (and a separately-built API Gateway) can trust a verified identity/role context without re-implementing login/session logic.

## Extension Configuration (final)

| Extension | Status |
|---|---|
| Security Baseline | Enabled — full blocking enforcement |
| Resiliency Baseline | Opted in initially, then downgraded to informational-only per explicit user request (avoid over-engineering) |
| Property-Based Testing | Enabled — Partial mode (PBT-02, 03, 07, 08, 09 only) |

## Out of Scope (this phase)

- API Gateway (request routing/reverse-proxying to backend microservices) — built as a **separate project** (explicit user decision, 2026-07-31); this service only issues/validates JWTs
- Product catalog access policy (e.g., public read access) — responsibility of the product service / separately-built API Gateway, not this service
- Apple and Facebook social login (deferred)
- Cross-region disaster recovery / multi-region deployment
- Manual admin IP block/unblock tooling
- Formal chaos engineering / DR game-day testing
