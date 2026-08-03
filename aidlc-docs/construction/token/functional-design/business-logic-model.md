# Business Logic Model — Unit: Token

## 1. 토큰 발급 (Issue) — US-301

**입력**: accountId, role (로그인 성공 시 Account/SocialLogin Unit이 호출)

**절차**:
1. 새 `familyId` 생성 (최초 로그인이므로 새 패밀리 시작)
2. Access Token(JWT) 생성 — 클레임: sub, role, familyId, jti, iat, exp(+15분). HS256으로 서명.
3. Refresh Token 원문 생성(랜덤 값) → 해시하여 `RefreshToken` 레코드 저장 (status=ACTIVE, expiresAt=+14일, previousTokenId=null)
4. Access Token 원문 + Refresh Token 원문을 클라이언트에 반환

## 2. 토큰 갱신/회전 (Refresh & Rotate) — US-302

**입력**: refreshToken(원문)

**절차**:
1. 원문을 해시해 `RefreshToken` 레코드 조회
2. 레코드 없음 또는 `status != ACTIVE` 또는 만료됨 → 3번(재사용 탐지) 분기로 이동 (없음/만료는 거부만, `ROTATED`/`REVOKED` 상태로 존재하면 재사용 시도로 간주)
3. 유효(ACTIVE, 미만료)하면:
   - 기존 레코드 `status = ROTATED`로 변경
   - 새 Refresh Token 발급 (`familyId` 동일 승계, `previousTokenId` = 기존 레코드 id)
   - 새 Access Token 발급 (동일 accountId, role, 새 jti)
   - 새 토큰 쌍 반환

## 3. 재사용 탐지 및 패밀리 무효화 (Reuse Detection) — US-303

**트리거**: 이미 `ROTATED` 또는 `REVOKED` 상태인 Refresh Token으로 갱신을 시도

**절차**:
1. 요청 거부 (재로그인 필요 안내만, 별도 알림 없음 — Q3:A)
2. 해당 레코드의 `familyId`에 속한 **모든** Refresh Token 레코드를 `status = REVOKED`로 일괄 변경
3. 그 패밀리로 이미 발급되어 아직 만료되지 않은 Access Token들은 자연 만료를 기다리는 대신, 다음 요청에서 거부되도록 하려면 블랙리스트 등록이 필요하지만 — Access Token은 jti 단위로만 블랙리스트가 가능하고 발급된 jti 목록을 별도로 추적하지 않으므로, **이번 범위에서는 패밀리의 Refresh Token만 즉시 무효화하고, 이미 발급된 Access Token은 자체 만료(최대 15분)까지 유효할 수 있음**을 알려진 제약으로 남긴다. (즉시성이 필요하면 로그아웃(4번) 경로처럼 개별 jti 블랙리스트가 필요 — Functional Design 범위를 넘는 트레이드오프이므로 Code Generation 단계에서 재확인 권장)

## 4. 로그아웃 (Logout) — US-304

**입력**: accessToken, refreshToken

**절차**:
1. refreshToken 해시로 레코드 조회 → `status = REVOKED`
2. accessToken의 jti를 파싱해 `blacklist:accessToken:{jti}` 키를 Redis에 등록 (TTL = accessToken 남은 만료 시간)
3. 이후 해당 accessToken으로 오는 모든 요청은 검증(5번) 단계에서 블랙리스트 확인으로 거부됨

## 5. 토큰 검증/인트로스펙션 (Validate) — FR-07

**입력**: accessToken(원문)

**절차**:
1. JWT 서명 검증 (HS256, 공유 비밀키)
2. 서명 유효 + 만료 전이면, `jti`로 Redis 블랙리스트 조회
3. 블랙리스트에 없으면 valid=true, accountId(sub)와 role 반환
4. 서명 무효/만료/블랙리스트 등록됨 중 하나라도 해당하면 valid=false

## 6. 비밀번호 변경 시 전체 무효화 (Account Unit과의 협력, US-104 지원)

**입력**: accountId

**절차**: 해당 accountId 소유의 `ACTIVE` 상태 Refresh Token 레코드를 모두 `REVOKED`로 변경 (재사용 탐지와 달리 패밀리 단위가 아니라 계정의 모든 패밀리 대상)
