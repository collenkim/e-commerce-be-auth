# NFR Requirements — Unit: Token

## Scalability

- 프로젝트 전역 NFR-03(수평 확장성)을 상속: Refresh Token 상태(MariaDB)와 블랙리스트(Redis)는 여러 인스턴스가 동시에 접근해도 정합성이 보장되어야 한다 (프로세스 내 메모리 캐시 금지).
- 별도의 용량 계획/부하 테스트는 이번 단계에서 설계하지 않는다 (Q2:A — 그린필드, 실측 데이터 없음).

## Performance

- 목표: 단일 인스턴스 기준 p99 응답 시간 100ms 이하 (발급/갱신/검증 API 공통, 표준적 목표 — Q2:A). 실측 트래픽 확보 후 재조정.
- `validate()` API는 외부 호출자(별도 게이트웨이 등)가 매 요청마다 호출할 수 있으므로 Redis 조회 1회 + JWT 서명 검증(로컬 연산)으로 구성해 왕복을 최소화한다 (DB 접근 없음).

## Availability / Reliability

- Resiliency Baseline은 정보성만 적용(블로킹 아님) — 별도 DR/failover 질문 없이 단일 리전 + Multi-AZ 기본값 사용 (프로젝트 전역 결정, `aidlc-state.md` 참고).
- **Fail-closed (SECURITY-15)**: `validate()` 수행 중 Redis 접근이 실패하면 "유효하지 않음"으로 간주해 거부한다. 가용성보다 보안을 우선한다.
- MariaDB 접근 실패 시(발급/회전 시 레코드 저장 불가) 해당 요청은 실패 처리한다 — 토큰을 추적할 수 없는 상태로 발급하지 않는다.

## Security Compliance (Security Baseline — 전체 강제)

| Rule | 상태 | 비고 |
|---|---|---|
| SECURITY-01 | Compliant | MariaDB/Redis 암호화 요구사항 문서화, 실제 설정은 Infrastructure Design에서 |
| SECURITY-02 | N/A | 이 Unit은 네트워크 중개 자원(LB/APIGW/CDN)을 소유하지 않음 |
| SECURITY-03 | Compliant | 구조화 로깅 요구, Access/Refresh Token 원문 로깅 금지 명시 |
| SECURITY-04 | N/A | HTML 서빙 없음 (JSON API 전용) |
| SECURITY-05 | Compliant | refreshToken/accessToken 입력에 대한 타입/길이 검증 요구사항 문서화, 세부 구현은 Code Generation |
| SECURITY-06 | N/A | 이 단계 범위 아님 — Infrastructure Design에서 평가 |
| SECURITY-07 | N/A | 이 단계 범위 아님 — Infrastructure Design에서 평가 |
| SECURITY-08 | Compliant | 매 요청마다 서버 사이드 토큰 검증(서명/만료/블랙리스트) 이미 설계에 반영됨 |
| SECURITY-09 | Compliant | 프로덕션 오류 응답은 일반 메시지만 반환 (스택트레이스/내부 정보 노출 금지) 요구사항 문서화 |
| SECURITY-10 | Compliant | 의존성 고정/취약점 스캔 요구사항을 `tech-stack-decisions.md`에 문서화 |
| SECURITY-11 | Compliant | Token 로직은 이미 별도 컴포넌트/Unit으로 격리됨. 참고: refresh/logout 엔드포인트에도 Rate Limit 적용 여부는 RateLimit Unit 설계에서 확정 필요(크로스 유닛 후속 항목, Token Unit 자체를 막지 않음) |
| SECURITY-12 | Compliant | 세션(토큰) 서버사이드 만료·로그아웃 즉시 무효화 이미 설계됨. MFA는 이번 phase 범위 밖(N/A 근거: 요구사항에 MFA 스토리 없음). 비밀키는 시크릿 매니저/환경변수로 주입 요구사항 문서화 |
| SECURITY-13 | Compliant | jjwt의 안전한 파싱(서명 미검증 토큰 거부) 사용, 토큰 상태 변경 감사 로그 요구사항 이미 문서화(`business-rules.md`) |
| SECURITY-14 | Compliant | 인증 실패/재사용 탐지 이벤트에 대한 알림 요구사항 문서화, 세부 알림 채널 설정은 Infrastructure Design |
| SECURITY-15 | Compliant | Fail-closed 기본값 문서화 (Redis 장애 시 거부) |

**Blocking findings**: 없음.

## PBT Compliance (Partial mode — PBT-02, 03, 07, 08, 09만 강제)

| Rule | 상태 | 비고 |
|---|---|---|
| PBT-09 | Compliant | 프레임워크 선택 완료 (jqwik) — `tech-stack-decisions.md` 참고 |
| PBT-02, 03, 07, 08 | N/A (이 단계 대상 아님) | Code Generation 단계에서 적용 (Enforcement Integration 표 기준) |

**Blocking findings**: 없음.
