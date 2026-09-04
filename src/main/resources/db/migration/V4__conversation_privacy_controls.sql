ALTER TABLE conversation
    ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE account
    ADD COLUMN vault_password_hash VARCHAR(64);

CREATE INDEX idx_conversation_owner_hidden_created
    ON conversation(owner_id, hidden, created_at DESC);
