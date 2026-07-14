CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE organization (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    branding_color VARCHAR(7) DEFAULT '#4F46E5',
    timezone VARCHAR(50) DEFAULT 'UTC',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE membership (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL REFERENCES organization(id),
    user_id UUID NOT NULL,   -- references app_user in identity service's
                              -- table — same physical DB (shared schema,
                              -- Phase 1), but no FK constraint across
                              -- service-owned tables by convention, even
                              -- though physically possible. Enforced at
                              -- the application layer instead.
    role VARCHAR(20) NOT NULL CHECK (role IN ('owner','admin','agent')),
    invited_at TIMESTAMPTZ,
    joined_at TIMESTAMPTZ,
    UNIQUE(org_id, user_id)
);

CREATE INDEX idx_membership_org ON membership(org_id);
CREATE INDEX idx_membership_user ON membership(user_id);

ALTER TABLE organization ENABLE ROW LEVEL SECURITY;
ALTER TABLE membership ENABLE ROW LEVEL SECURITY;

CREATE POLICY org_isolation ON organization
    USING (id = current_setting('app.current_org_id', true)::UUID);

CREATE POLICY membership_isolation ON membership
    USING (org_id = current_setting('app.current_org_id', true)::UUID);