CREATE TABLE IF NOT EXISTS push_subscription (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    endpoint TEXT NOT NULL UNIQUE,
    p256dh TEXT NOT NULL,
    auth TEXT NOT NULL,
    user_agent TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_success_at TIMESTAMP,
    last_failure_at TIMESTAMP,
    CONSTRAINT fk_push_subscription_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_push_subscription_active_user
    ON push_subscription(active, user_id);

CREATE INDEX IF NOT EXISTS idx_match_open_notification
    ON match(result, match_time, championship_id);
