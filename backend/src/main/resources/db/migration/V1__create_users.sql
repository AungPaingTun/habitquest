CREATE TABLE users (
    id             BIGSERIAL    PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    display_name   VARCHAR(50)  NOT NULL,
    -- IANA time zone (e.g. 'Asia/Yangon'); used to decide what "today" means for check-ins
    timezone       VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
