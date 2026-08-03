# Functional Design Plan — Unit: Account

**Unit 책임**: 회원가입, 이메일 인증, 비밀번호 재설정, 로그인 자격증명 검증 (`unit-of-work.md` Unit 2 참고)
**관련 스토리**: US-101(가입), US-102(이메일 인증), US-103(로그인 — 자격증명 검증 주 소유, 토큰 발급은 Token Unit 위임), US-104(비밀번호 재설정)
**의존 Unit**: Token(비밀번호 재설정 시 전체 무효화 호출, 로그인 성공 시 토큰 발급 위임)
**입력 문서**: `requirements.md`(FR-01, FR-10, FR-11, NFR-01), `stories.md`, `application-design/components.md`

## Execution Checklist

- [x] Step A: Resolve business logic questions below (gate)
- [x] Step B: Generate `business-logic-model.md`
- [x] Step C: Generate `business-rules.md`
- [x] Step D: Generate `domain-entities.md`

## Questions (GATE)

### Question 1: 비밀번호 정책
SECURITY-12는 "최소 8자, 유출된 비밀번호 목록과 대조"를 요구합니다. 유출 목록 대조까지 이번 범위에 포함할까요?

A) 최소 8자 + 기본적인 복잡도(문자/숫자 조합 등)만 검증 — 외부 유출 목록 대조(예: Have I Been Pwned API 연동)는 이번 범위 밖, 향후 별도 검토

B) 외부 유출 목록 대조까지 포함 — 외부 API 연동 설계 필요 (범위/복잡도 증가)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: 이메일 인증/비밀번호 재설정 토큰 만료 시간
A) 이메일 인증 토큰 24시간 / 비밀번호 재설정 토큰 30분 (일반적인 기본값)

B) 직접 지정 — [Answer]: 뒤에 값을 적어주세요

[Answer]: A

### Question 3: 이메일 미인증 계정의 처리
회원가입 후 이메일 인증을 하지 않은 계정은 어떻게 할까요?

A) 별도 만료/삭제 없음 — 미인증 상태로 무기한 보관, 재발송(US-102)으로 언제든 인증 가능 (가장 단순, 이번 범위에서 배치 작업 없음)

B) 일정 기간 후 자동 삭제/만료 처리 필요 — 별도 배치 설계 필요

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## 사전 결정 사항 (질문 없이 적용)

- **이메일 정규화**: 저장/조회 시 소문자로 정규화(대소문자 무관 중복 가입 방지)
- **로그인 실패 메시지**: US-103 AC에 따라 계정 존재 여부를 유추할 수 없는 동일한 형태의 오류만 반환(사용자 열거 공격 방지, SECURITY-12)
- **로그인 시 토큰 발급**: Account Unit은 자격증명만 검증하고, 성공 시 `TokenIssuanceService.issue(accountId, role)`를 호출한다(Token Unit 의존, 이미 완료된 Unit). Rate Limit 적용은 RateLimit Unit이 아직 빌드되지 않아 이번 Unit에서는 통합하지 않는다 — RateLimit Unit 빌드 시 로그인/가입 엔드포인트에 필터를 결합한다(알려진 통합 지점, Token Unit에서 사용한 패턴과 동일).
