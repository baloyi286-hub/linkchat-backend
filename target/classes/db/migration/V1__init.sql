CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE account (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  display_name VARCHAR(100) NOT NULL,
  invite_code VARCHAR(64) NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE visitor_profile (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  browser_token_hash VARCHAR(128) NOT NULL UNIQUE,
  display_name VARCHAR(100) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE visitor_image (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  visitor_id UUID NOT NULL REFERENCES visitor_profile(id) ON DELETE CASCADE,
  storage_key VARCHAR(500) NOT NULL,
  original_name VARCHAR(255),
  content_type VARCHAR(100),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_visitor_image_visitor ON visitor_image(visitor_id);

CREATE TABLE conversation (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
  visitor_id UUID NOT NULL REFERENCES visitor_profile(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  status VARCHAR(30) NOT NULL DEFAULT 'OPEN'
);
CREATE INDEX idx_conversation_owner_created ON conversation(owner_id, created_at DESC);
CREATE INDEX idx_conversation_visitor_created ON conversation(visitor_id, created_at DESC);

CREATE TABLE chat_message (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
  sender_type VARCHAR(20) NOT NULL,
  body TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_message_conversation_created ON chat_message(conversation_id, created_at);

INSERT INTO account(display_name, invite_code)
VALUES ('Demo Owner', 'demo')
ON CONFLICT (invite_code) DO NOTHING;
