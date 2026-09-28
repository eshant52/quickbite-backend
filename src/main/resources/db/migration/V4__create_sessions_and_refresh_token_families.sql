ALTER TABLE refresh_tokens
DROP
CONSTRAINT fk_refresh_tokens_on_user;

CREATE TABLE refresh_token_families
(
    id                UUID NOT NULL,
    created_at        TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at        TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    session_id        UUID NOT NULL,
    revoked_at        TIMESTAMP(6) WITHOUT TIME ZONE,
    reuse_detected_at TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT pk_refresh_token_families PRIMARY KEY (id)
);

CREATE TABLE sessions
(
    id           UUID         NOT NULL,
    created_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    user_id      UUID         NOT NULL,
    device_name  VARCHAR(100) NOT NULL,
    device_os    VARCHAR(100) NOT NULL,
    client_type  CLIENT_TYPE  NOT NULL,
    ip           VARCHAR(50)  NOT NULL,
    user_agent   TEXT,
    login_at     TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    last_used_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    expires_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    revoked_at   TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT pk_sessions PRIMARY KEY (id)
);

ALTER TABLE refresh_tokens
    ADD revoked_at TIMESTAMP(6) WITHOUT TIME ZONE;

ALTER TABLE refresh_tokens
    ADD used_at TIMESTAMP(6) WITHOUT TIME ZONE;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT FK_REFRESH_TOKENS_ON_FAMILY FOREIGN KEY (family_id) REFERENCES refresh_token_families (id);

ALTER TABLE refresh_token_families
    ADD CONSTRAINT FK_REFRESH_TOKEN_FAMILIES_ON_SESSION FOREIGN KEY (session_id) REFERENCES sessions (id);

ALTER TABLE sessions
    ADD CONSTRAINT FK_SESSIONS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE refresh_tokens
DROP
COLUMN client_type;

ALTER TABLE refresh_tokens
DROP
COLUMN device_name;

ALTER TABLE refresh_tokens
DROP
COLUMN last_used_at;

ALTER TABLE refresh_tokens
DROP
COLUMN os;

ALTER TABLE refresh_tokens
DROP
COLUMN revoked;

ALTER TABLE refresh_tokens
DROP
COLUMN user_id;

ALTER TABLE refresh_tokens
    ALTER COLUMN family_id SET NOT NULL;