# Logical Components — Unit: SocialLogin

| 논리적 컴포넌트 | 역할 | 비고 |
|---|---|---|
| `NormalizingOAuth2UserService` | provider별 원시 사용자 정보를 `{provider, providerUserId, email}`로 정규화 | Spring Security `OAuth2UserService` 커스텀 구현 |
| `SocialLoginSuccessHandler` | 로그인/가입/연동대기 판단, JWT 발급, 세션 무효화, 프런트엔드 리다이렉트 | `AuthenticationSuccessHandler` 커스텀 구현 |
| `SocialLoginFailureHandler` | 실패 시 일반화된 오류로 프런트엔드 리다이렉트 | `AuthenticationFailureHandler` 커스텀 구현 |
| `SocialAccountRepository` (Spring Data JPA) | `SocialAccount` CRUD | MariaDB |
| `PendingSocialLinkRepository` (Spring Data JPA) | `PendingSocialLink` CRUD | MariaDB |
| `SocialLoginService` | 로그인/가입/연동판단/연동확인 오케스트레이션 | Account Unit(3개 신규 메서드), Token Unit(`issue`) 호출 |

## 큐/캐시 등 인프라 요소

- 없음(새 인프라 자원 불필요)
