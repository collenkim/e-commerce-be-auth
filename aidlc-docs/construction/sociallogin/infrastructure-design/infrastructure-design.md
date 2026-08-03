# Infrastructure Design — Unit: SocialLogin

공통 결정은 `shared-infrastructure.md` 참고 — 이 Unit은 새 컨테이너/데이터스토어가 필요 없다(기존 MariaDB 재사용).

## 논리 컴포넌트 → 인프라 매핑

| 논리 컴포넌트 | 인프라 매핑 |
|---|---|
| `SocialAccountRepository`, `PendingSocialLinkRepository` | MariaDB(공유 인스턴스, `social_account`/`pending_social_link` 테이블) |
| `NormalizingOAuth2UserService`, `SocialLoginSuccessHandler`, `SocialLoginFailureHandler`, `SocialLoginService` | 인프라 자원 아님 — 애플리케이션 로직 |
| Kakao/Naver/Google (외부) | 이 서비스가 관리하지 않는 외부 provider — HTTPS로 통신 |

## 로컬 개발 환경 설정 값 (초안)

| 항목 | 값 |
|---|---|
| Provider 자격증명 | 환경변수 `KAKAO_CLIENT_ID`/`KAKAO_CLIENT_SECRET`, `NAVER_CLIENT_ID`/`NAVER_CLIENT_SECRET`, `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` — 로컬 개발 시 각 provider 개발자 콘솔에서 발급받아 `.env`에 채워야 실제 동작(값 없이는 소셜 로그인 기능만 비활성) |
| 프런트엔드 콜백 URL | 환경변수 `OAUTH2_FRONTEND_REDIRECT_URI`, 로컬 기본값 `http://localhost:3000/oauth2/callback` |
| 연동 확인 토큰 TTL | 10분 (설정값으로 노출) |
