ALTER TABLE app_user ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE app_user ADD COLUMN google_id VARCHAR(255) UNIQUE;

CREATE INDEX idx_app_user_google_id ON app_user(google_id);