# Integration Test Instructions

## Purpose
Verify the 5 Units work together against **real infrastructure** (MariaDB, Redis, RabbitMQ) — something the automated test suite could not fully verify during Code Generation (H2 substituted for MariaDB; Redis/RabbitMQ were never live). `integration-points.md` explicitly flags this as unverified — this is where it gets closed out.

## Setup Integration Test Environment

### 1. Configure environment
```bash
cp .env.example .env
# Fill in DB_PASSWORD and JWT_HMAC_SECRET at minimum (any values work locally).
# Leave OAuth2 client id/secret blank unless testing social login specifically.
```

### 2. Start Required Services
```bash
docker-compose up --build
```
- **First thing to verify**: the `auth-service` container starts and stays up (does not crash-loop). This directly resolves the open item in `integration-points.md`: "실제 인프라로 기동하는지" and "빈 client-id가 실제 docker-compose 환경에서도 문제없는지" (the empty-credential OAuth2 registration case was only verified against H2/no-Redis/no-RabbitMQ in `ECommerceBeAuthApplicationTests`).
- Flyway should run `V1__create_schema.sql` (single consolidated schema file — see schema management policy note below) against the real MariaDB on startup — check container logs for `Successfully applied 1 migration`.

### 3. Configure Service Endpoint
```bash
export API_URL=http://localhost:28080
```

## Run Integration Test Scenarios

These are manual (curl/Postman) scenarios — no automated integration-test suite exists yet (Gradle has no `integrationTest` source set configured; all current tests run under `./gradlew test`, using H2/mocks). Consider adding one if this project continues past this phase.

### Scenario 1: Account → Token (signup, verify, login, refresh, logout)
1. `POST $API_URL/api/auth/signup` `{"email":"...", "password":"Password1"}` → expect 201. Check RabbitMQ management UI (`http://localhost:25673`, guest/guest) — no queue is bound, but publishing should not error (check `auth-service` logs for absence of `Failed to publish email event` warnings).
2. Retrieve the verification token — since no email consumer exists, read it from the `email_verification_token` table directly (`docker exec` into `mariadb` or connect with a client) or add a temporary log statement.
3. `POST $API_URL/api/auth/email/verify` `{"token": "..."}` → expect 204.
4. `POST $API_URL/api/auth/login` → expect 200 with `accessToken`/`refreshToken`.
5. `POST $API_URL/api/auth/token/refresh` with the refresh token → expect 200 with a **new** token pair; confirm the old refresh token now fails if reused (expect 401, and check logs for the reuse-detection warning).
6. `POST $API_URL/api/auth/logout` with `Authorization: Bearer <accessToken>` and the current refresh token → expect 204. Confirm the access token is now rejected: `POST $API_URL/internal/tokens/validate` `{"accessToken": "..."}` → expect `{"valid": false}`.

### Scenario 2: RateLimit → Account (signup/login abuse protection)
1. Call `POST /api/auth/login` with wrong credentials 5+ times within 10 minutes for the same account → expect the 6th attempt to return 429 `ACCOUNT_TEMPORARILY_LOCKED` (US-402).
2. Call `POST /api/auth/signup` 11+ times within 60 seconds from the same client → expect 429 `RATE_LIMIT_EXCEEDED`, then eventually 403 `IP_BLOCKED` once the violation threshold is hit (US-401/403).
3. Confirm blocks clear automatically after the TTL (15 min) without any manual action (US-405) — check Redis directly (`docker exec -it <redis> redis-cli KEYS 'block:*'`) to watch keys expire.

### Scenario 3: Authorization → Account (admin role change, access control)
1. Manually promote a test account to `ADMIN` (directly in MariaDB, since there's no bootstrap-admin mechanism — see `integration-points.md` known gaps).
2. Log in as that admin, then `PATCH /api/admin/accounts/{accountId}/role` `{"role":"SELLER"}` with the admin's access token → expect 204.
3. Attempt the same call with a non-admin `USER` token → expect 403 `ACCESS_DENIED`.
4. Attempt the same call with no `Authorization` header → expect 401 `UNAUTHENTICATED`.
5. Confirm the role change requires the target user to re-login before it's reflected in their own token (documented limitation — role is fixed at refresh-token issuance time).

### Scenario 4: SocialLogin (requires real OAuth2 app credentials — optional)
Only runnable if `.env` has real `GOOGLE_CLIENT_ID`/`SECRET` (Kakao/Naver require their own developer console apps + redirect URI registration).
1. Visit `http://localhost:28080/oauth2/authorization/google` in a browser.
2. Complete the Google login. Confirm redirect to `OAUTH2_FRONTEND_REDIRECT_URI` with `#access_token=...&refresh_token=...` in the URL fragment.
3. Repeat login with the same Google account with an email that also has a local password account — confirm redirect instead contains `#linkRequired=true&linkToken=...`, and that `POST /api/auth/social/link/confirm` with the correct password completes the link.

## Cleanup
```bash
docker-compose down -v   # -v also removes the mariadb-data volume for a clean slate
```

## Results Tracking
Record actual results (pass/fail per scenario, and any bugs found) back into `integration-points.md` under a new "Integration Test Results" section — following this project's established pattern of treating that file as the running source of truth for cross-unit issues.
