-- =============================================================================
-- 스키마 관리 정책
-- =============================================================================
-- 기능 개발이 진행 중인 동안은 이 파일 하나로 전체 스키마를 관리한다.
-- V1, V2 ... 같은 순차 버전 분리는 "기능 개발이 다 끝나고 스키마가 확정된 이후"에
-- 발생하는 변경 사항을 이력 관리하기 위한 용도다 - 지금처럼 개발 중에는 새 버전을
-- 추가하지 않고 이 V1 파일 자체를 계속 고쳐서 최신 스키마 하나만 유지한다.
-- 상세 근거: aidlc-docs/construction/integration-points.md "프로젝트 정책: DB 스키마 관리"
--
-- PK로 AUTO_INCREMENT 대신 UUID(CHAR(36))를 쓰는 이유
-- 1) ID 추측/열거(enumeration) 공격 방지 - 인증 서비스 특성상 순차 정수 ID는
--    다른 사용자의 리소스 ID를 쉽게 추측하게 해준다(SECURITY-11, defense in depth).
-- 2) 수평 확장 대비(NFR-03) - AUTO_INCREMENT는 DB가 유일한 카운터 소유자여야 해서
--    나중에 DB를 나누거나 다중 인스턴스로 쓸 때 충돌 조율이 필요하지만 UUID는 애초에
--    충돌이 없다.
-- 3) 삽입 전에 ID를 알아야 하는 경우가 있다 - 예: refresh_token.previous_token_id가
--    이전 레코드의 id를 참조하는데, UUID는 애플리케이션에서 DB 왕복 없이 바로
--    생성/참조할 수 있다(JPA `GenerationType.UUID`).
-- 트레이드오프: CHAR(36) PK는 INT/BIGINT보다 인덱스가 크고 삽입이 약간 느리다.
-- 지금 규모에서는 무시할 만해 보안 이점을 우선했다.
--
-- 모든 테이블/컬럼에 COMMENT를 붙인다 - SQL 파일의 -- 주석과 달리 DB에 실제로
-- 저장되어 `SHOW FULL COLUMNS`, information_schema, DBeaver 등 DB 툴에서
-- 소스코드를 안 봐도 바로 확인할 수 있다.
-- =============================================================================

-- =============================================================================
-- Account Unit
-- =============================================================================

CREATE TABLE account (
    id                CHAR(36)     NOT NULL
        COMMENT '계정 식별자. Token/SocialLogin 등 다른 테이블의 account_id가 이 값을 참조한다(FK 제약 없음 - Unit 간 결합도를 낮추기 위한 의도적 선택, 참조 무결성은 애플리케이션 레벨에서 보장).',
    email             VARCHAR(254) NOT NULL
        COMMENT '로그인 식별자. 애플리케이션에서 항상 소문자로 정규화한 뒤 저장/조회한다(대소문자만 다른 이메일로 중복 가입 방지).',
    password_hash     VARCHAR(100) NOT NULL
        COMMENT 'BCrypt 해시(cost factor 12). 원문 비밀번호는 절대 저장하지 않는다(SECURITY-12). 소셜 로그인 전용 계정은 사용자가 알 수 없는 무작위 값이 들어간다.',
    role              VARCHAR(20)  NOT NULL
        COMMENT 'USER | SELLER | ADMIN. 가입 시 항상 USER로 시작, 관리자 API로만 변경 가능. 이미 발급된 토큰에는 재로그인 전까지 변경이 반영되지 않는다(알려진 제약).',
    status            VARCHAR(30)  NOT NULL
        COMMENT 'PENDING_VERIFICATION | ACTIVE. 이메일/비밀번호 가입은 PENDING_VERIFICATION으로 시작해 이메일 인증 후 ACTIVE로 전환되고, 소셜 로그인 신규 가입은 provider가 이미 이메일 소유를 확인한 것으로 간주해 즉시 ACTIVE로 생성된다.',
    email_verified_at TIMESTAMP(6) NULL
        COMMENT '이메일 인증 완료 시각. 미인증 상태면 NULL.',
    created_at        TIMESTAMP(6) NOT NULL
        COMMENT '가입(레코드 생성) 시각.',
    PRIMARY KEY (id),
    CONSTRAINT uq_account_email UNIQUE (email)
) ENGINE = InnoDB
  COMMENT = '이메일/비밀번호 계정(소셜 전용 계정 포함). Account Unit 소유.';

CREATE TABLE email_verification_token (
    id           CHAR(36)     NOT NULL
        COMMENT '토큰 레코드 식별자.',
    account_id   CHAR(36)     NOT NULL
        COMMENT '대상 계정. account.id를 참조(애플리케이션 레벨 참조, FK 제약 없음).',
    token_hash   VARCHAR(64)  NOT NULL
        COMMENT '토큰 원문의 SHA-256 해시(com.ecommerce.auth.shared.OpaqueTokenGenerator). 원문은 저장하지 않는다(SECURITY-03) - 이메일 발송 이벤트(RabbitMQ)에만 실려 나간다.',
    expires_at   TIMESTAMP(6) NOT NULL
        COMMENT '발급 + 24시간. 이 시각 이후에는 사용 불가.',
    consumed_at  TIMESTAMP(6) NULL
        COMMENT '사용(인증 완료) 또는 재발급으로 인한 무효화 시각. NULL이면 아직 유효. 한 계정에 새 토큰을 발급하면 기존 미소비 토큰은 전부 이 값이 채워진다(한 번에 하나의 유효한 인증 링크만 존재하도록).',
    PRIMARY KEY (id),
    CONSTRAINT uq_evt_token_hash UNIQUE (token_hash)
) ENGINE = InnoDB
  COMMENT = '이메일 인증 토큰(해시만 저장, TTL 24시간). Account Unit 소유.';

CREATE INDEX idx_evt_account_id ON email_verification_token (account_id);

CREATE TABLE password_reset_token (
    id           CHAR(36)     NOT NULL
        COMMENT '토큰 레코드 식별자.',
    account_id   CHAR(36)     NOT NULL
        COMMENT '대상 계정. account.id를 참조.',
    token_hash   VARCHAR(64)  NOT NULL
        COMMENT '토큰 원문의 SHA-256 해시. 원문은 저장하지 않는다(SECURITY-03).',
    expires_at   TIMESTAMP(6) NOT NULL
        COMMENT '발급 + 30분(이메일 인증 토큰보다 짧다 - 비밀번호 재설정은 더 민감한 동작이므로).',
    consumed_at  TIMESTAMP(6) NULL
        COMMENT '사용 또는 재발급으로 인한 무효화 시각. NULL이면 아직 유효.',
    PRIMARY KEY (id),
    CONSTRAINT uq_prt_token_hash UNIQUE (token_hash)
) ENGINE = InnoDB
  COMMENT = '비밀번호 재설정 토큰(해시만 저장, TTL 30분). Account Unit 소유.';

CREATE INDEX idx_prt_account_id ON password_reset_token (account_id);

-- =============================================================================
-- Token Unit
-- =============================================================================

CREATE TABLE refresh_token (
    id                 CHAR(36)     NOT NULL
        COMMENT 'Refresh Token 레코드 식별자.',
    account_id         CHAR(36)     NOT NULL
        COMMENT '소유 계정. account.id를 참조.',
    role               VARCHAR(20)  NOT NULL
        COMMENT '발급 시점의 역할(USER/SELLER/ADMIN). 이후 관리자가 역할을 바꿔도 이 값은 그대로 유지되며, 회전(refresh) 시 새 토큰도 이 값을 그대로 승계한다(역할 변경이 재로그인 전까지 미반영되는 알려진 제약의 근거).',
    token_hash         VARCHAR(64)  NOT NULL
        COMMENT 'Refresh Token 원문의 SHA-256 해시. 원문은 클라이언트에만 전달되고 저장하지 않는다.',
    family_id          CHAR(36)     NOT NULL
        COMMENT '토큰 패밀리 식별자. 최초 로그인 시 새로 생성되고 이후 회전(refresh)마다 그대로 승계된다. 재사용 탐지 시 이 값이 같은 레코드 전부가 한꺼번에 REVOKED 처리된다(도난된 토큰 체인 전체 무효화).',
    status             VARCHAR(20)  NOT NULL
        COMMENT 'ACTIVE(사용 가능) | ROTATED(회전으로 대체됨) | REVOKED(로그아웃/재사용탐지/비밀번호변경으로 무효화). ROTATED/REVOKED 상태의 토큰이 다시 사용되면 재사용(탈취 의심)으로 간주해 family_id가 같은 레코드를 전부 REVOKED 처리한다.',
    issued_at          TIMESTAMP(6) NOT NULL
        COMMENT '발급 시각.',
    expires_at         TIMESTAMP(6) NOT NULL
        COMMENT '발급 + 14일.',
    previous_token_id  CHAR(36)     NULL
        COMMENT '이 토큰이 어떤 토큰을 회전(대체)해서 발급됐는지 - 패밀리 체인 추적용. 최초 발급된 토큰은 NULL.',
    PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token_token_hash UNIQUE (token_hash)
) ENGINE = InnoDB
  COMMENT = 'Refresh Token 메타데이터(원문 미저장). 상태와 무관하게 삭제하지 않고 영구 보관한다(감사 목적). Access Token(JWT) 자체는 무상태라 이 DB에 저장하지 않는다 - 검증은 서명 확인 + Redis 블랙리스트 조회로 이뤄진다. Token Unit 소유.';

-- 재사용 탐지 시 family_id로 같은 패밀리의 모든 레코드를 조회/무효화하는 경로.
CREATE INDEX idx_refresh_token_family_id ON refresh_token (family_id);
-- 비밀번호 변경 시 "계정의 모든 ACTIVE 토큰"을 조회하는 경로(전체 무효화용).
CREATE INDEX idx_refresh_token_account_id_status ON refresh_token (account_id, status);

-- =============================================================================
-- SocialLogin Unit
-- =============================================================================

CREATE TABLE social_account (
    id                CHAR(36)     NOT NULL
        COMMENT '연결 레코드 식별자.',
    provider          VARCHAR(20)  NOT NULL
        COMMENT 'KAKAO | NAVER | GOOGLE.',
    provider_user_id  VARCHAR(191) NOT NULL
        COMMENT 'provider가 발급하는 사용자 고유 ID(우리 쪽 account.id가 아니라 provider 측 ID).',
    account_id        CHAR(36)     NOT NULL
        COMMENT '연결된 계정. account.id를 참조.',
    linked_at         TIMESTAMP(6) NOT NULL
        COMMENT '연동(최초 연결) 완료 시각.',
    PRIMARY KEY (id),
    CONSTRAINT uq_social_account_provider_user UNIQUE (provider, provider_user_id)
) ENGINE = InnoDB
  COMMENT = '소셜 provider 아이덴티티 ↔ Account 연결. 한 계정이 여러 provider를 동시에 연결하는 것은 허용된다(제약 없음). SocialLogin Unit 소유.';

-- 계정 탈퇴/조회 시 "이 계정에 연결된 모든 소셜 아이덴티티"를 찾는 경로.
CREATE INDEX idx_social_account_account_id ON social_account (account_id);

CREATE TABLE pending_social_link (
    id                  CHAR(36)     NOT NULL
        COMMENT '연동 대기 요청 식별자.',
    existing_account_id CHAR(36)     NOT NULL
        COMMENT '연동 대상이 되는 기존 계정. account.id를 참조.',
    provider            VARCHAR(20)  NOT NULL
        COMMENT 'KAKAO | NAVER | GOOGLE.',
    provider_user_id    VARCHAR(191) NOT NULL
        COMMENT 'provider가 발급하는 사용자 고유 ID.',
    email               VARCHAR(254) NOT NULL
        COMMENT 'provider가 반환한 이메일(정규화됨). 참고용 - 실제 연동 대상 판단은 existing_account_id로 이뤄진다.',
    token_hash          VARCHAR(64)  NOT NULL
        COMMENT '연동 확인 토큰 원문의 SHA-256 해시. 원문은 저장하지 않는다(SECURITY-03).',
    expires_at          TIMESTAMP(6) NOT NULL
        COMMENT '발급 + 10분(짧게 유지 - 방치된 연동 요청이 오래 남아있지 않도록).',
    consumed_at         TIMESTAMP(6) NULL
        COMMENT '연동 확인 완료 또는 만료로 인한 소비 시각. NULL이면 아직 유효.',
    PRIMARY KEY (id),
    CONSTRAINT uq_pending_social_link_token_hash UNIQUE (token_hash)
) ENGINE = InnoDB
  COMMENT = '소셜 로그인 시 동일 이메일 기존 계정이 발견됐지만 아직 사용자 확인(기존 비밀번호 재입력)을 거치지 않아 자동 연동하지 않은 "대기 중" 연동 요청. 확인 완료 시 social_account에 정식 레코드가 생기고 이 레코드는 consumed 처리된다. SocialLogin Unit 소유.';

-- 특정 계정에 대해 대기 중인 연동 요청을 조회하는 경로.
CREATE INDEX idx_pending_social_link_account_id ON pending_social_link (existing_account_id);
