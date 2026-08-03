# Business Logic Model — Unit: Account

## 1. 회원가입 (Sign Up) — US-101

**입력**: email, rawPassword

**절차**:
1. email을 소문자로 정규화
2. 정규화된 email로 기존 계정 존재 여부 확인 → 있으면 중복 가입 오류
3. 비밀번호 정책 검증(Q1:A — 최소 8자 + 기본 복잡도)
4. BCrypt로 해시
5. `Account`(status=`PENDING_VERIFICATION`, role=`USER`) 저장
6. `EmailVerificationToken` 발급(원문 생성 → 해시 저장, TTL 24시간)
7. `EmailVerificationRequested` 이벤트 발행 (토큰 원문 포함)

## 2. 이메일 인증 (Verify Email) — US-102

**입력**: 검증 토큰(원문)

**절차**:
1. 해시로 `EmailVerificationToken` 조회
2. 없음/만료/이미 소비됨 → 오류 반환(재발송 안내)
3. 유효하면: 토큰 `consumedAt` 설정, `Account.status = ACTIVE`, `emailVerifiedAt = now`

**재발송**: 새 토큰 발급 시 해당 계정의 기존 미소비 토큰들을 전부 `consumedAt` 처리(무효화)해 한 번에 하나의 유효 링크만 존재하도록 한다.

## 3. 로그인 자격증명 검증 (Login) — US-103

**입력**: email, rawPassword

**절차**:
1. email 정규화 후 계정 조회 — 없으면 실패(일반 오류, 계정 열거 방지)
2. 비밀번호 불일치 → 실패(동일한 일반 오류)
3. `Account.status != ACTIVE`(이메일 미인증) → 실패("이메일 인증이 필요합니다" 안내, US-102 AC3 — 이 경우는 자격증명 오류와 달리 구체적으로 안내)
4. 위 모두 통과 → `TokenIssuanceService.issue(accountId, role)` 호출(Token Unit 위임), 결과 반환

**참고**: Rate Limit(US-401, US-402)은 RateLimit Unit이 아직 빌드되지 않아 이 Unit에서는 통합하지 않는다 — 알려진 통합 지점(코드 요약에 기록).

## 4. 비밀번호 재설정 요청 (Request Password Reset) — US-104

**입력**: email

**절차**:
1. email 정규화 후 계정 조회
2. 계정이 없어도 동일한 성공 응답을 반환한다(로그인 실패 메시지와 동일한 원칙 — 계정 존재 여부 비노출). 실제 이벤트 발행은 계정이 존재할 때만 수행.
3. 계정이 있으면: 해당 계정의 기존 미소비 재설정 토큰을 전부 무효화, 새 `PasswordResetToken` 발급(TTL 30분), `PasswordResetRequested` 이벤트 발행

## 5. 비밀번호 재설정 실행 (Reset Password) — US-104

**입력**: 재설정 토큰(원문), 새 비밀번호

**절차**:
1. 해시로 `PasswordResetToken` 조회 — 없음/만료/이미 소비됨 → 오류
2. 새 비밀번호 정책 검증(1번과 동일 규칙) → BCrypt 해시로 `Account.passwordHash` 갱신
3. 토큰 `consumedAt` 설정
4. `TokenIssuanceService.revokeAllForAccount(accountId)` 호출(Token Unit 위임) — 기존 Refresh Token 전체 무효화
