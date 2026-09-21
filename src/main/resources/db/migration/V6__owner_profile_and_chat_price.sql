ALTER TABLE account ADD COLUMN chat_price NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (chat_price >= 0);
CREATE TABLE owner_image (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
  storage_key VARCHAR(500) NOT NULL,
  original_name VARCHAR(255),
  content_type VARCHAR(100),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_owner_image_owner ON owner_image(owner_id);
