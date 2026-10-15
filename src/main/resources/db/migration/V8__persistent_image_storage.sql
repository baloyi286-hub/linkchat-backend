CREATE TABLE image_blob (storage_key VARCHAR(100) PRIMARY KEY, content_type VARCHAR(100) NOT NULL, image_data BYTEA NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now());
