CREATE TABLE owner_push_subscription (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    firebase_installation_id VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_owner_push_subscription_account_id
    ON owner_push_subscription(account_id);
