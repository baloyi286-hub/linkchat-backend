ALTER TABLE visitor_profile ADD COLUMN account_id UUID NULL REFERENCES account(id) ON DELETE SET NULL;
CREATE INDEX idx_visitor_profile_account ON visitor_profile(account_id);
