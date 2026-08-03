# NFR Requirements — Unit: SocialLogin

Token/Account Unit에서 확정된 공통 사항(수평 확장성, fail-closed 원칙, PBT 프레임워크=jqwik, 표준 커넥션 풀)을 상속한다.

## Performance (예외 명시)

- **p99 100ms 목표의 예외**: 소셜 로그인은 카카오/네이버/구글 외부 API 호출(토큰 교환 + 사용자정보 조회, 최소 2회 왕복)을 포함하므로 나머지 서비스의 표준 목표(p99 100ms)를 적용하지 않는다. 외부 API 지연은 이 서비스가 통제할 수 없다 — 별도 목표를 설정하지 않고 provider의 정상 응답 시간에 의존한다(그린필드, 실측 데이터 없음).
- Spring Security `oauth2-client`의 기본 HTTP 클라이언트 타임아웃을 사용한다(커스텀 타임아웃 튜닝 없음 — 최소 회복탄력성 방침과 일치).

## Availability / Reliability

- 외부 provider 호출 실패(네트워크 오류, 4xx/5xx)는 Spring Security의 기본 인증 실패 처리로 이어지며, 이 Unit의 커스텀 실패 핸들러가 `{redirectUri}#error=social_login_failed`로 리다이렉트한다(재시도 없음, 프로젝트 최소 회복탄력성 방침).

## Security Compliance (Security Baseline — 전체 강제)

| Rule | 상태 | 비고 |
|---|---|---|
| SECURITY-01 | Compliant | provider와의 통신은 HTTPS(OAuth2 표준), MariaDB 암호화 요구사항 상속 |
| SECURITY-03 | Compliant | provider access token/인가코드는 로그에 남기지 않음 |
| SECURITY-05 | Compliant | 연동 확인 요청(linkToken, password) 입력 검증 요구사항 |
| SECURITY-08 | Compliant | 연동 확인은 비밀번호 재검증으로 소유권 증명 |
| SECURITY-09 | Compliant | 실패 시 일반화된 오류만 프런트엔드에 전달 |
| SECURITY-10 | Compliant | `spring-boot-starter-oauth2-client` 등 의존성 고정/스캔 |
| SECURITY-12 | Compliant | provider client-secret은 환경변수로 주입(하드코딩 없음), 소셜 전용 계정은 무작위 비밀번호 해시 |
| **추가 규칙(정제)** | Compliant | Google의 `email_verified=false` 응답 시 자동 `ACTIVE` 생성 경로를 적용하지 않고 일반 가입과 동일하게 이메일 인증을 요구한다(신규 규칙, business-rules.md에 반영 필요 — Code Generation에서 구현) |
| 나머지(02,04,06,07,11,13,14,15) | N/A 또는 상속 | Token/Account Unit과 동일 근거로 N/A이거나 이미 상속된 원칙 적용 |

**Blocking findings**: 없음.

## PBT Compliance

| Rule | 상태 | 비고 |
|---|---|---|
| PBT-09 | Compliant | jqwik 상속 |
| PBT-02, 03, 07, 08 | N/A (이 단계 대상 아님) | Code Generation 단계 |
