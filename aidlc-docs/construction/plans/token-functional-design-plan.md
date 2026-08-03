# Functional Design Plan — Unit: Token

**Unit 책임**: JWT 발급/회전/재사용탐지/무효화, 외부 검증(introspection) API (`unit-of-work.md` Unit 1 참고)
**관련 스토리**: US-301(발급), US-302(갱신/회전), US-303(재사용 탐지/패밀리 무효화), US-304(로그아웃/즉시 무효화)
**입력 문서**: `requirements.md`(NFR-01, NFR-02, NFR-03), `application-design/components.md`, `application-design/component-methods.md`

## Execution Checklist

- [x] Step A: Resolve business logic questions below (gate — needs user answers)
- [x] Step B: Generate `business-logic-model.md`
- [x] Step C: Generate `business-rules.md`
- [x] Step D: Generate `domain-entities.md`

## Questions (GATE — answer before Step B proceeds)

### Question 1: Access/Refresh Token 만료 시간
US-301은 "짧은 만료 시간"/"긴 만료 시간"만 명시합니다. 구체적인 값은?

A) Access 15분 / Refresh 14일 (일반적인 업계 기본값)

B) Access 30분 / Refresh 30일

C) 직접 지정 — [Answer]: 뒤에 값을 적어주세요

[Answer]: A

### Question 2: JWT 서명 알고리즘
A) HMAC (HS256) — 대칭키, 이 서비스 혼자 발급/검증하므로 가장 단순 (외부 게이트웨이/서비스가 검증하려면 비밀키 공유 필요)

B) 비대칭키 (RS256/ES256) — 이 서비스만 개인키로 서명, 외부(별도 게이트웨이·다른 백엔드 서비스)는 공개키로 자체 검증 가능 (비밀키 공유 불필요, FR-07 검증 API와도 병행 가능)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: 토큰 패밀리(Family) 재사용 탐지 시 사용자 영향
US-303: 재사용 탐지 시 토큰 패밀리 전체 무효화. 이때 사용자에게는 어떻게 안내할까요?

A) 단순 재로그인 요구 — 특별한 알림 없이 다음 API 호출부터 401, 클라이언트가 재로그인 화면으로 유도

B) 보안 이벤트로 기록 + (향후) 사용자에게 알림 발송 — 지금은 감사 로그만 남기고 알림 발송은 이번 범위에서 제외

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: 토큰 검증/인트로스펙션 API의 호출자 인증
외부(별도 게이트웨이, 다른 백엔드 서비스)가 호출하는 `TokenComponent.validate()` API 자체는 어떻게 보호할까요?

A) 별도 인증 없음 — 내부망(사설 네트워크) 전제로 열어둠 (가장 단순, 지금 단계에 적합)

B) 서비스 간 API 키/mTLS 등으로 별도 인증 — 별도 설계 필요

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5: 만료된 Refresh Token의 데이터 보관 정책
사용됨/만료됨/무효화된 Refresh Token 레코드를 MariaDB에서 어떻게 처리할까요?

A) 영구 보관 (감사 목적, SECURITY-03/14 관련) — 별도 삭제 배치 없음, 지금 단계에서 스토리지 정리는 범위 밖

B) TTL 기반 주기적 삭제 필요 — 별도 배치/스케줄러 설계 필요

X) Other (please describe after [Answer]: tag below)

[Answer]: A
