CREATE TABLE habits (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    icon          VARCHAR(16),
    -- points earned per check-in, chosen by the user
    points        INT          NOT NULL CHECK (points BETWEEN 1 AND 100),
    frequency     VARCHAR(10)  NOT NULL CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    -- check-ins needed per period: always 1 for DAILY, 1-7 per week, 1-31 per month
    target_count  INT          NOT NULL DEFAULT 1,
    start_date    DATE         NOT NULL,
    -- NULL means the habit runs forever
    end_date      DATE,
    archived      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT habits_target_fits_frequency CHECK (
        (frequency = 'DAILY'   AND target_count = 1) OR
        (frequency = 'WEEKLY'  AND target_count BETWEEN 1 AND 7) OR
        (frequency = 'MONTHLY' AND target_count BETWEEN 1 AND 31)
    ),
    CONSTRAINT habits_end_after_start CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_habits_user_archived ON habits (user_id, archived);
