-- ============================================================
--  CS2 ARENA — boshlang'ich sxema
-- ============================================================

CREATE TABLE users (
    id                      BIGSERIAL PRIMARY KEY,
    steam_id                VARCHAR(20)  NOT NULL UNIQUE,
    nickname                VARCHAR(32)  NOT NULL,
    avatar_url              VARCHAR(512),
    country                 VARCHAR(64)  NOT NULL DEFAULT 'O''zbekiston',
    city                    VARCHAR(64),
    language                VARCHAR(8)   NOT NULL DEFAULT 'uz',
    role                    VARCHAR(16)  NOT NULL DEFAULT 'PLAYER',
    elo                     INT          NOT NULL DEFAULT 1000,
    matches_played          INT          NOT NULL DEFAULT 0,
    wins                    INT          NOT NULL DEFAULT 0,
    kills                   INT          NOT NULL DEFAULT 0,
    deaths                  INT          NOT NULL DEFAULT 0,
    assists                 INT          NOT NULL DEFAULT 0,
    headshots               INT          NOT NULL DEFAULT 0,
    damage                  BIGINT       NOT NULL DEFAULT 0,
    rounds_played           INT          NOT NULL DEFAULT 0,
    premium_plan            VARCHAR(16),
    premium_until           TIMESTAMPTZ,
    banned                  BOOLEAN      NOT NULL DEFAULT FALSE,
    ban_reason              VARCHAR(255),
    banned_until            TIMESTAMPTZ,
    notify_match_sound      BOOLEAN      NOT NULL DEFAULT TRUE,
    notify_friend_requests  BOOLEAN      NOT NULL DEFAULT TRUE,
    notify_tournaments      BOOLEAN      NOT NULL DEFAULT TRUE,
    notify_email            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_elo ON users (elo DESC);

CREATE TABLE wallets (
    user_id     BIGINT PRIMARY KEY REFERENCES users (id),
    balance     BIGINT      NOT NULL DEFAULT 0 CHECK (balance >= 0),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE transactions (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users (id),
    amount         BIGINT       NOT NULL,
    type           VARCHAR(24)  NOT NULL,
    description    VARCHAR(255),
    reference      VARCHAR(64),
    balance_after  BIGINT       NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_transactions_user ON transactions (user_id, created_at DESC);

CREATE TABLE payments (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT       NOT NULL REFERENCES users (id),
    provider           VARCHAR(16)  NOT NULL,
    amount             BIGINT       NOT NULL CHECK (amount > 0),
    status             VARCHAR(16)  NOT NULL,
    provider_trans_id  VARCHAR(64),
    provider_state     INT,
    create_time        BIGINT,
    perform_time       BIGINT,
    cancel_time        BIGINT,
    cancel_reason      INT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_payment_provider_trans UNIQUE (provider, provider_trans_id)
);
CREATE INDEX idx_payments_user ON payments (user_id, created_at DESC);

CREATE TABLE game_servers (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(64)  NOT NULL,
    region         VARCHAR(32)  NOT NULL,
    ip             VARCHAR(64)  NOT NULL,
    port           INT          NOT NULL,
    rcon_password  VARCHAR(128) NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_server_address UNIQUE (ip, port)
);

CREATE TABLE matches (
    id              BIGSERIAL PRIMARY KEY,
    status          VARCHAR(16)  NOT NULL,
    mode            VARCHAR(16)  NOT NULL,
    team_size       INT          NOT NULL,
    map             VARCHAR(32),
    region          VARCHAR(32)  NOT NULL,
    password        VARCHAR(64),
    anti_cheat      BOOLEAN      NOT NULL DEFAULT FALSE,
    overtime        BOOLEAN      NOT NULL DEFAULT TRUE,
    open_lobby      BOOLEAN      NOT NULL DEFAULT FALSE,
    spectators      BOOLEAN      NOT NULL DEFAULT TRUE,
    invite_code     VARCHAR(16)  NOT NULL UNIQUE,
    host_id         BIGINT       NOT NULL REFERENCES users (id),
    server_id       BIGINT       REFERENCES game_servers (id),
    team_a_name     VARCHAR(32)  NOT NULL DEFAULT 'Team A',
    team_b_name     VARCHAR(32)  NOT NULL DEFAULT 'Team B',
    score_a         INT,
    score_b         INT,
    winner          VARCHAR(1),
    veto_turn       VARCHAR(1),
    veto_deadline   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ
);
CREATE INDEX idx_matches_status ON matches (status);

CREATE TABLE match_players (
    id          BIGSERIAL PRIMARY KEY,
    match_id    BIGINT      NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    user_id     BIGINT      NOT NULL REFERENCES users (id),
    team        VARCHAR(1)  NOT NULL,
    kills       INT         NOT NULL DEFAULT 0,
    deaths      INT         NOT NULL DEFAULT 0,
    assists     INT         NOT NULL DEFAULT 0,
    headshots   INT         NOT NULL DEFAULT 0,
    damage      INT         NOT NULL DEFAULT 0,
    elo_before  INT,
    elo_change  INT,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_match_player UNIQUE (match_id, user_id)
);
CREATE INDEX idx_match_players_user ON match_players (user_id);

CREATE TABLE match_vetoes (
    id          BIGSERIAL PRIMARY KEY,
    match_id    BIGINT      NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    map         VARCHAR(32) NOT NULL,
    team        VARCHAR(1),
    action      VARCHAR(8)  NOT NULL,
    position    INT         NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_messages (
    id          BIGSERIAL PRIMARY KEY,
    match_id    BIGINT       NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    user_id     BIGINT       REFERENCES users (id),
    channel     VARCHAR(8)   NOT NULL,
    team        VARCHAR(1),
    text        VARCHAR(500) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_chat_match ON chat_messages (match_id, created_at);

CREATE TABLE news (
    id          BIGSERIAL PRIMARY KEY,
    category    VARCHAR(32)  NOT NULL,
    title       VARCHAR(160) NOT NULL,
    body        TEXT,
    published   BOOLEAN      NOT NULL DEFAULT TRUE,
    author_id   BIGINT       REFERENCES users (id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE admin_actions (
    id          BIGSERIAL PRIMARY KEY,
    admin_id    BIGINT       NOT NULL REFERENCES users (id),
    action      VARCHAR(32)  NOT NULL,
    target      VARCHAR(64),
    details     VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_admin_actions_created ON admin_actions (created_at DESC);
