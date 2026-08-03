# NFR Requirements — Unit: Account

Token Unit NFR Requirements에서 확정된 공통 사항(수평 확장성, p99 100ms 기본 목표, fail-closed 원칙, PBT 프레임워크=jqwik)을 그대로 상속한다.

## Scalability / Performance

- Account 상태(MariaDB)는 인스턴스 로컬 캐시 없이 공유 DB로 관리 — 수평 확장 시에도 정합성 유지(NFR-03 상속).
- BCrypt cost factor 12는 로그인/가입 지연에 의도적인 지연(수십 ms)을 추가하지만 브루트포스 저항성을 위한 표준 트레이드오프로 수용한다.

## Availability / Reliability

- **이메일 이벤트 발행 실패는 non-blocking**: 회원가입/비밀번호 재설정 요청 자체를 실패시키지 않는다(로깅만). SECURITY-15의 fail-closed 원칙은 인가/보안 결정에 적용되며, 알림성 부가 채널(이메일 발송)에는 적용하지 않는다 — 이 예외를 명시적으로 문서화한다.
- RabbitMQ 발행 실패 시 재시도는 Spring AMQP 기본 설정을 사용(과도한 커스텀 재시도 로직 도입 안 함, 프로젝트의 최소 회복탄력성 방침과 일치).

## Security Compliance (Security Baseline — 전체 강제)

| Rule | 상태 | 비고 |
|---|---|---|
| SECURITY-01 | Compliant | MariaDB/RabbitMQ 암호화 요구사항, 실제 설정은 Infrastructure Design |
| SECURITY-02 | N/A | 네트워크 중개 자원 없음 |
| SECURITY-03 | Compliant | 비밀번호/토큰 원문 로깅 금지 (business-rules.md 상속) |
| SECURITY-04 | N/A | HTML 서빙 없음 |
| SECURITY-05 | Compliant | email/password/token 입력 검증 요구사항 문서화 |
| SECURITY-06, 07 | N/A | Infrastructure Design 대상 |
| SECURITY-08 | Compliant | 계정 존재 비노출 규칙, 로그인/재설정 응답 일관성 이미 설계됨 |
| SECURITY-09 | Compliant | 일반화된 오류 응답 요구사항 |
| SECURITY-10 | Compliant | 의존성 고정/스캔 요구사항, `tech-stack-decisions.md` |
| SECURITY-11 | Compliant | 계정 로직은 별도 Unit/컴포넌트로 격리됨 |
| SECURITY-12 | Compliant | BCrypt(cost 12), 최소 8자+복잡도, 계정 열거 방지, 하드코딩 비밀 없음(비밀키는 환경변수) |
| SECURITY-13 | Compliant | 토큰 안전 파싱, 계정 상태 변경 감사 로그 요구사항 |
| SECURITY-14 | Compliant | 반복 가입 실패/재설정 요청 남용에 대한 알림 요구사항 문서화, 도구는 Infrastructure Design에서 |
| SECURITY-15 | Compliant (명시적 예외 포함) | 인가 관련 실패는 fail-closed, 이메일 발행 실패는 non-blocking으로 명시적 예외 처리 |

**Blocking findings**: 없음.

## PBT Compliance (Partial mode — PBT-02, 03, 07, 08, 09만 강제)

| Rule | 상태 | 비고 |
|---|---|---|
| PBT-09 | Compliant | jqwik 상속 (Token Unit에서 이미 선택) |
| PBT-02, 03, 07, 08 | N/A (이 단계 대상 아님) | Code Generation 단계에서 적용 |

**Blocking findings**: 없음.
