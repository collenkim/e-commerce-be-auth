# Build Instructions

## Prerequisites
- **Build Tool**: Gradle (Kotlin DSL), via the included wrapper (`./gradlew` / `gradlew.bat`) — no local Gradle install needed
- **JDK**: Java 17 (toolchain-managed by Gradle; a compatible JDK must be discoverable, but the build resolves the exact 17 toolchain itself)
- **Dependencies**: Resolved automatically from Maven Central on first build (Spring Boot 4.0.7 BOM, jjwt, jqwik, H2, etc. — see `build.gradle.kts`)
- **Environment Variables** (required only to actually *run* the app, not to build/test — see `.env.example`):
  - `DB_PASSWORD`, `JWT_HMAC_SECRET` (no default — build/test will still succeed without these; only `bootRun` against real infra needs them)
  - `RABBITMQ_USERNAME`/`PASSWORD` (default `guest`/`guest`)
  - `GOOGLE_CLIENT_ID`/`SECRET`, `KAKAO_CLIENT_ID`/`SECRET`, `NAVER_CLIENT_ID`/`SECRET` (blank by default — social login simply won't function, everything else does)
- **System Requirements**: No special requirements for build/test (H2 in-memory substitutes for MariaDB in tests). Running the full stack via `docker-compose.yml` needs Docker.

## Build Steps

### 1. Install Dependencies
```bash
./gradlew dependencies
```
(Or simply proceed to step 3 — Gradle resolves dependencies automatically on first compile/test/build.)

### 2. Configure Environment
Not required for build/compile/test. For running against real infrastructure:
```bash
cp .env.example .env
# edit .env with real DB_PASSWORD / JWT_HMAC_SECRET / (optional) OAuth2 credentials
```

### 3. Build All Units (all 5 Units live in one Gradle module — see `unit-of-work.md`)
```bash
./gradlew clean build
```
This compiles, runs all tests, and packages the application.

### 4. Verify Build Success
- **Expected Output**: `BUILD SUCCESSFUL` — verified on 2026-08-03: `9 actionable tasks: 9 executed`, 0 failures.
- **Build Artifacts**:
  - `build/libs/e-commerce-be-auth-0.0.1-SNAPSHOT.jar` — executable Spring Boot fat jar
  - `build/libs/e-commerce-be-auth-0.0.1-SNAPSHOT-plain.jar` — plain jar without dependencies
  - `build/test-results/test/*.xml`, `build/reports/tests/test/index.html` — test reports
- **Common Warnings**: None currently blocking. A Gradle configuration-cache suggestion appears at the end of every build — informational only, safe to ignore.

## Troubleshooting

### Build Fails with Dependency Errors
- **Cause**: No network access to Maven Central, or a corrupted Gradle cache.
- **Solution**: Verify network access; retry with `./gradlew build --refresh-dependencies`.

### Build Fails with Compilation Errors
- **Cause**: JDK version mismatch (must support Java 17 toolchain resolution).
- **Solution**: Ensure a JDK 17+ is installed and discoverable by Gradle's toolchain provisioning (or set `org.gradle.java.installations.auto-download=true`, the Gradle default).

### `bootRun` / Docker Compose fails to start (not covered by `./gradlew build`)
See `integration-test-instructions.md` — this is a genuinely unverified step (no live MariaDB/Redis/RabbitMQ were available during Code Generation).
