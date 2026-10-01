CREATE TABLE prizes (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    icon        VARCHAR(16),
    -- locked once created (PRD §5.5); the app never updates this column
    cost        INT          NOT NULL CHECK (cost BETWEEN 1 AND 100000),
    archived    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_prizes_user_archived ON prizes (user_id, archived);

-- One row each time a prize is redeemed.
CREATE TABLE redemptions (
    id            BIGSERIAL   PRIMARY KEY,
    prize_id      BIGINT      NOT NULL REFERENCES prizes (id) ON DELETE CASCADE,
    user_id       BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    cost_at_time  INT         NOT NULL CHECK (cost_at_time > 0),
    redeemed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_redemptions_user_prize ON redemptions (user_id, prize_id);

ALTER TABLE point_transactions
    ADD CONSTRAINT fk_point_transactions_redemption
    FOREIGN KEY (redemption_id) REFERENCES redemptions (id) ON DELETE SET NULL;
