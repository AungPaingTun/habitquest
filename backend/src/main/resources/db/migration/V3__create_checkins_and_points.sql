-- One row per habit per day it was done. Points are copied in, so editing the habit later never changes history.
CREATE TABLE habit_logs (
    id              BIGSERIAL   PRIMARY KEY,
    habit_id        BIGINT      NOT NULL REFERENCES habits (id) ON DELETE CASCADE,
    user_id         BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    log_date        DATE        NOT NULL,
    points_awarded  INT         NOT NULL CHECK (points_awarded >= 0),
    bonus_awarded   INT         NOT NULL DEFAULT 0 CHECK (bonus_awarded >= 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- "once per day per habit", enforced by the database as well as the service
    CONSTRAINT uq_habit_logs_habit_day UNIQUE (habit_id, log_date)
);

CREATE INDEX idx_habit_logs_user_date ON habit_logs (user_id, log_date);

-- The points ledger: every change to a user's points is one row. Balance = SUM(amount).
CREATE TABLE point_transactions (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    amount       INT          NOT NULL CHECK (amount <> 0),
    type         VARCHAR(20)  NOT NULL CHECK (type IN ('EARN', 'STREAK_BONUS', 'UNDO', 'REDEEM')),
    habit_id     BIGINT       REFERENCES habits (id) ON DELETE SET NULL,
    log_date     DATE,
    -- set for REDEEM rows; the foreign key is added with the redemptions table in V4
    redemption_id BIGINT,
    -- human-readable line for the history page, e.g. "Eat healthy" or "Redeemed: Hotpot"
    description  VARCHAR(150) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_point_transactions_user_created ON point_transactions (user_id, created_at DESC);
