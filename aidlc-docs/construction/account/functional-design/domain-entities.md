# Domain Entities — Unit: Account

## Account (MariaDB)

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | UUID/PK | 계정 식별자 — Token Unit의 `RefreshToken.accountId`와 같은 값 공간을 공유 |
| `email` | string, unique | 소문자로 정규화되어 저장 |
| `passwordHash` | string | BCrypt |
| `role` | enum (`Role`, `com.ecommerce.auth.shared`) | 가입 시 항상 `USER`. SELLER/ADMIN 승격은 이 Unit 범위 밖(향후 관리자 기능) |
| `status` | enum | `PENDING_VERIFICATION` \| `ACTIVE` |
| `emailVerifiedAt` | timestamp, nullable | 인증 완료 시각 |
| `createdAt` | timestamp | 가입 시각 |

**보관 정책 (Q3:A)**: `PENDING_VERIFICATION` 상태로 무기한 보관 — 별도 만료/삭제 배치 없음.

## EmailVerificationToken (MariaDB)

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | UUID/PK | |
| `accountId` | FK | |
| `tokenHash` | string, unique | 원문은 저장하지 않음(SECURITY-03) |
| `expiresAt` | timestamp | 발급 + 24시간 (Q2:A) |
| `consumedAt` | timestamp, nullable | 사용(인증 완료) 또는 재발급으로 인한 무효화 시각 |

## PasswordResetToken (MariaDB)

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | UUID/PK | |
| `accountId` | FK | |
| `tokenHash` | string, unique | |
| `expiresAt` | timestamp | 발급 + 30분 (Q2:A) |
| `consumedAt` | timestamp, nullable | |

## 이메일 발송 이벤트 (메시지 큐 발행, Application Design Q5:B)

| 이벤트 | 페이로드 | 트리거 |
|---|---|---|
| `EmailVerificationRequested` | accountId, email, 검증 토큰 원문(1회성) | 가입, 재발송 |
| `PasswordResetRequested` | accountId, email, 재설정 토큰 원문(1회성) | 재설정 요청 |

Consumer는 이 프로젝트 범위 밖(Application Design 결정) — 발행 채널만 구현한다.
