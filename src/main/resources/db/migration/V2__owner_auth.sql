ALTER TABLE account ADD COLUMN auth_subject VARCHAR(255);
CREATE UNIQUE INDEX uq_account_auth_subject ON account(auth_subject) WHERE auth_subject IS NOT NULL;
