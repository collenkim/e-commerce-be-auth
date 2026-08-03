# Security Test Instructions

Security Baseline (SECURITY-01..15) is enabled with full blocking enforcement for this project (`requirements.md` NFR-04). Each unit's NFR Requirements stage self-assessed compliance per rule (see `construction/*/nfr-requirements/nfr-requirements.md`); this document is for **independently verifying** those self-assessments now that all code exists, plus items that were explicitly deferred as non-blocking.

## 1. Dependency Vulnerability Scanning (SECURITY-10 — flagged non-blocking gap)
Not yet wired into CI/CD (Authorization Unit NFR Requirements flagged this explicitly). To close it:
```bash
# Option A: OWASP Dependency-Check Gradle plugin (add to build.gradle.kts)
# Option B: enable GitHub Dependabot (.github/dependabot.yml) if this repo is hosted on GitHub
```
No scanner is currently configured — running one for the first time here is expected to surface findings; triage before treating any as blocking.

## 2. Authentication / Authorization Testing (SECURITY-08, US-501/502/603)
Automated coverage exists (`SecurityFilterChainIntegrationTest`), but only for the unauthenticated-request case. Manually verify the cases automated tests could not (no valid ADMIN/USER JWTs were mintable without live infra):
- Valid `USER` token calling `/api/admin/**` → expect 403 (see `integration-test-instructions.md` Scenario 3).
- Valid `ADMIN` token calling any protected endpoint → expect success.
- Expired/tampered token on any protected endpoint → expect 401.
- Blacklisted (logged-out) token reused → expect 401 (Scenario 1, step 6).

## 3. Input Validation Testing (SECURITY-05)
Every request DTO across all 5 units uses Bean Validation (`@NotBlank`, `@Email`, `@Size`, `@NotNull` on enums). Spot-check with malformed payloads:
```bash
curl -X POST $API_URL/api/auth/signup -H "Content-Type: application/json" -d '{"email":"not-an-email","password":"short"}'
# expect 400 with field-level validation errors
```

## 4. Credential/Secret Exposure Check (SECURITY-12)
```bash
grep -rn "password\|secret\|client_secret" src/main/resources/application.properties
```
Confirm every credential-shaped property uses `${ENV_VAR}` or `${ENV_VAR:default}` with a *non-secret* default (e.g., `guest` for local RabbitMQ) — never a real secret committed to source. Verified during Code Generation; re-check after any future config change.

## 5. Error Response Hygiene (SECURITY-09)
Every `@RestControllerAdvice`/security handler across the 5 units returns `{"code": "...", "message": "..."}` — confirm none leak stack traces:
```bash
curl -X POST $API_URL/api/auth/login -d 'not-json' -H "Content-Type: application/json"
# expect a generic 400, no stack trace, no internal class names in the body
```

## 6. CORS Verification (SECURITY-08, Authorization Unit)
```bash
curl -X OPTIONS $API_URL/api/auth/login -H "Origin: http://evil.example.com" \
  -H "Access-Control-Request-Method: POST" -i
# expect no Access-Control-Allow-Origin header for a disallowed origin (only the configured frontend origin should be echoed back)
```

## Status
Automated: `SecurityFilterChainIntegrationTest` (unauthenticated-request path). Everything else in this document is **manual, unexecuted** — needs a live environment (`docker-compose up`) and cannot be verified from source/unit tests alone. Track results in `integration-points.md`.
