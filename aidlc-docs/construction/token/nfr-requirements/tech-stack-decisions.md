# Tech Stack Decisions — Unit: Token

| 영역 | 선택 | 근거 |
|---|---|---|
| JWT 라이브러리 | `io.jsonwebtoken:jjwt` (jjwt-api/impl/jackson) | Q1:A — 발급과 검증을 모두 이 서비스가 직접 제어; HS256 대칭키 방식과 가장 단순하게 맞음 |
| 서명 알고리즘 | HMAC-SHA256 (HS256) | Functional Design Q2:A 상속 |
| 영속성 (Refresh Token) | Spring Data JPA + MariaDB | 프로젝트 전역 NFR-01 상속 |
| 블랙리스트/캐시 | Spring Data Redis (Lettuce 클라이언트) | 프로젝트 전역 NFR-02 상속 |
| PBT 프레임워크 (PBT-09) | jqwik | JUnit 5 통합, Java 표준 스택과 자연스럽게 맞음 (property-based-testing.md 권장 표) |
| 비밀키 관리 | 환경변수/시크릿 매니저로 주입 (소스코드/설정파일에 하드코딩 금지) | SECURITY-12 |
| 의존성 관리 | Gradle 버전 카탈로그 또는 명시적 버전 고정, 취약점 스캔 도구(예: OWASP Dependency-Check 또는 GitHub Dependabot) 구성 | SECURITY-10 — 세부 CI 연동은 Build and Test 단계에서 확정 |

## 미해결/후속 항목

- **Rate Limit 적용 범위**: Token Unit의 refresh/logout 엔드포인트가 RateLimit Unit의 "인증 엔드포인트" 범위에 포함되는지는 RateLimit Unit 설계 시 확정한다 (FR-08 참고).
