# NFR Requirements — Unit: Authorization

이전 4개 Unit의 공통 사항을 상속한다. 이 Unit이 `SecurityFilterChain`을 완성하는 마지막 Unit이므로, 아래 Security Compliance 표는 **이 Unit 자체 범위**뿐 아니라 **서비스 전체 관점의 종합 점검**을 포함한다.

## CORS (Q1:A)

- 허용 오리진: `app.social-login.frontend-redirect-uri`에서 추출한 오리진(scheme+host+port)만 허용. 로컬 기본값 `http://localhost:3000`.
- 인증된 엔드포인트에 와일드카드(`*`) 오리진을 사용하지 않는다(SECURITY-08).

## Security Compliance (Security Baseline — 전체 강제, 서비스 전체 종합 점검)

| Rule | 상태 | 비고 |
|---|---|---|
| SECURITY-01 | Compliant | 요구사항은 전 Unit에 문서화됨. 실제 암호화 설정은 클라우드 배포 대상이 정해진 뒤 Infra 재검토 필요(로컬 개발 단계는 평문 — 알려진 제약) |
| SECURITY-02 | N/A | 네트워크 중개 자원 없음(게이트웨이는 별도 프로젝트) |
| SECURITY-03 | Compliant | 전 Unit에서 일관되게 준수(민감정보 로깅 없음) |
| SECURITY-04 | N/A | HTML 서빙 없음 |
| SECURITY-05 | Compliant | 전 Unit Bean Validation 사용 |
| SECURITY-06, 07 | N/A | 클라우드 대상 미정, 로컬 개발 단계 |
| **SECURITY-08** | **Compliant (이 Unit에서 최초로 실제 강제됨)** | `JwtAuthenticationFilter` 배선 + `authorizeHttpRequests` + 관리자 역할 검증 + CORS 비와일드카드. 이전까지는 "설계는 됐지만 배선 안 됨" 상태였음 |
| SECURITY-09 | Compliant | 전 Unit 일관된 일반화 오류 응답. 이 Unit도 커스텀 401/403 핸들러로 동일 형식 유지 |
| SECURITY-10 | **Non-blocking 미완료 항목** | 의존성 취약점 스캐너가 아직 CI/CD에 실제로 연결되지 않음(도구 선정만 이전에 문서화) — Build and Test 단계에서 반영 필요, 이 Unit이 막는 사항은 아님 |
| SECURITY-11 | Compliant | 보안 로직 격리, Rate Limit 구현됨 |
| SECURITY-12 | Compliant | BCrypt, 세션/토큰 만료, 브루트포스 방지, 비밀 하드코딩 없음. MFA는 이번 phase 범위 밖(요구사항 자체에 없음) |
| SECURITY-13 | Compliant | 안전한 역직렬화(Jackson/jjwt 표준), 감사 로그 요구사항 문서화 |
| SECURITY-14 | Compliant (도구 선정 보류) | 알림 요구사항은 문서화됨, 실제 모니터링 스택은 클라우드 대상 미정으로 보류(Token Unit Infra Design에서 이미 결정된 사항) |
| SECURITY-15 | Compliant (명시적 예외 2건) | Token/Account/SocialLogin은 fail-closed(토큰 검증) 또는 non-blocking(이메일 발행) 원칙, RateLimit만 의도적 fail-open — 모두 문서화된 의도적 결정 |

**Blocking findings**: 없음. (SECURITY-10은 non-blocking으로 Build and Test에 이관)

## PBT Compliance

| Rule | 상태 | 비고 |
|---|---|---|
| PBT-09 | Compliant | jqwik 상속 |
| PBT-02, 03, 07, 08 | N/A (이 단계 대상 아님) | Code Generation 단계 |
