# 04 — Database Design

**Status:** Draft v1.0
**References:** 01-Product-Vision.md, 02-SRS.md, 03-System-Architecture.md
**Last updated:** 2026-07-01

---

## 1. Design Principles

- **Multi-tenancy:** shared schema, every tenant-scoped table carries `org_id`. Postgres Row-Level Security (RLS) policies enforce isolation at the DB layer as defense-in-depth beyond application-level filtering.
- **Right-sized for Phase 1:** ~18 tables, not 40–60. Every table below maps directly to an FR in Doc 02. Extra tables (tickets, tags, macros, CSAT) are deliberately excluded until Phase 2 — adding them now would be designing for requirements that don't exist yet.
- **UUIDs** for all primary keys (safer to expose in APIs than sequential IDs, and multi-tenant-friendly).
- **Soft deletes** (`deleted_at` timestamp) on tenant-owned data where accidental loss would be costly (documents, conversations); hard deletes elsewhere.

---

## 2. Entity-Relationship Diagram (Phase 1)

```
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│    User        │        │  Organization │        │  Membership   │
│──────────────│        │──────────────│        │──────────────│
│ id (PK)        │        │ id (PK)        │        │ id (PK)        │
│ email          │◄──────┤│ name           │┌──────►│ org_id (FK)    │
│ password_hash  │        │ slug           ││        │ user_id (FK)   │
│ email_verified │        │ branding_color ││        │ role           │
│ created_at     │        │ timezone       ││        │ invited_at     │
└──────────────┘        │ created_at     ││        │ joined_at      │
                          └──────────────┘│        └──────────────┘
                                            │
                                            │  org_id FK on every
                                            │  table below
                                            │
      ┌─────────────────────────────────────┼──────────────────────────┐
      ▼                                      ▼                          ▼
┌──────────────┐                    ┌──────────────┐          ┌──────────────┐
│ ApiKey         │                    │ KnowledgeDoc   │          │ Conversation   │
│──────────────│                    │──────────────│          │──────────────│
│ id (PK)        │                    │ id (PK)        │          │ id (PK)        │
│ org_id (FK)    │                    │ org_id (FK)    │          │ org_id (FK)    │
│ key_hash       │                    │ filename       │          │ visitor_id     │
│ created_at     │                    │ file_type      │          │ status         │
│ revoked_at     │                    │ status         │          │ assigned_agent │
└──────────────┘                    │ uploaded_by    │          │ mode (ai/human)│
                                       │ created_at     │          │ created_at     │
                                       │ deleted_at     │          │ deleted_at     │
                                       └───────┬──────┘          └───────┬──────┘
                                                │                          │
                                                ▼                          ▼
                                       ┌──────────────┐          ┌──────────────┐
                                       │ KnowledgeChunk │          │ Message        │
                                       │──────────────│          │──────────────│
                                       │ id (PK)        │          │ id (PK)        │
                                       │ document_id(FK)│          │ conversation_id│
                                       │ org_id (FK)    │          │ sender_type    │
                                       │ content        │          │ sender_id      │
                                       │ embedding      │          │ content        │
                                       │  (vector)      │          │ source_chunks  │
                                       │ chunk_index    │          │  (jsonb, if AI)│
                                       └──────────────┘          │ created_at     │
                                                                    └──────────────┘

┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ FAQEntry       │   │ AsyncJob       │   │ Notification   │   │ AuditLog       │
│──────────────│   │──────────────│   │──────────────│   │──────────────│
│ id (PK)        │   │ id (PK)        │   │ id (PK)        │   │ id (PK)        │
│ org_id (FK)    │   │ job_type       │   │ org_id (FK)    │   │ org_id (FK)    │
│ question       │   │ payload (jsonb)│   │ user_id (FK)   │   │ actor_user_id  │
│ answer         │   │ status         │   │ type           │   │ action         │
│ created_by     │   │ attempts       │   │ sent_at        │   │ entity_type    │
└──────────────┘   │ run_after      │   │ read_at        │   │ entity_id      │
                     │ created_at     │   └──────────────┘   │ metadata(jsonb)│
                     │ completed_at   │                       │ created_at     │
                     └──────────────┘                       └──────────────┘

┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ PasswordReset  │   │ EmailVerify    │   │ RefreshToken   │
│ Token          │   │ Token          │   │──────────────│
│──────────────│   │──────────────│   │ id (PK)        │
│ id (PK)        │   │ id (PK)        │   │ user_id (FK)   │
│ user_id (FK)   │   │ user_id (FK)   │   │ token_hash     │
│ token_hash     │   │ token_hash     │   │ expires_at     │
│ expires_at     │   │ expires_at     │   │ revoked_at     │
│ used_at        │   │ used_at        │   └──────────────┘
└──────────────┘   └──────────────┘
```

**18 tables total:** User, Organization, Membership, ApiKey, KnowledgeDoc, KnowledgeChunk, Conversation, Message, FAQEntry, AsyncJob, Notification, AuditLog, PasswordResetToken, EmailVerifyToken, RefreshToken — plus 3 reserved-but-not-yet-populated: `Subscription` (Phase 2 billing, table exists so FK relationships don't need to change), `WidgetConfig` (if branding config outgrows the columns on Organization), `AiResponseLog` (if Message.source_chunks needs to become its own table for querying).

---

## 3. Key Table Definitions

### `organization`
```sql
CREATE TABLE organization (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    branding_color VARCHAR(7) DEFAULT '#4F46E5',
    timezone VARCHAR(50) DEFAULT 'UTC',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
```

### `membership` (join table: user ↔ org, with role)
```sql
CREATE TABLE membership (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL REFERENCES organization(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('owner','admin','agent')),
    invited_at TIMESTAMPTZ,
    joined_at TIMESTAMPTZ,
    UNIQUE(org_id, user_id)
);
CREATE INDEX idx_membership_org ON membership(org_id);
CREATE INDEX idx_membership_user ON membership(user_id);
```

### `knowledge_chunk` (the pgvector table)
```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE knowledge_chunk (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES knowledge_doc(id) ON DELETE CASCADE,
    org_id UUID NOT NULL REFERENCES organization(id),
    content TEXT NOT NULL,
    embedding VECTOR(768),   -- dimension depends on embedding model chosen
    chunk_index INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chunk_org ON knowledge_chunk(org_id);
CREATE INDEX idx_chunk_embedding ON knowledge_chunk
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

### `conversation` / `message`
```sql
CREATE TABLE conversation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL REFERENCES organization(id),
    visitor_id VARCHAR(100) NOT NULL,   -- anonymous session identifier
    status VARCHAR(20) NOT NULL DEFAULT 'open' CHECK (status IN ('open','resolved')),
    mode VARCHAR(10) NOT NULL DEFAULT 'ai' CHECK (mode IN ('ai','human')),
    assigned_agent_id UUID REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_conversation_org ON conversation(org_id);

CREATE TABLE message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    sender_type VARCHAR(10) NOT NULL CHECK (sender_type IN ('visitor','ai','agent','system')),
    sender_id UUID,   -- null for visitor/ai/system
    content TEXT NOT NULL,
    source_chunks JSONB,   -- populated when sender_type = 'ai'
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_message_conversation ON message(conversation_id);
```

### `async_job` (the lightweight queue replacement)
```sql
CREATE TABLE async_job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type VARCHAR(50) NOT NULL,   -- 'document_ingestion' | 'ai_response'
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','processing','completed','failed')),
    attempts INT NOT NULL DEFAULT 0,
    run_after TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);
CREATE INDEX idx_async_job_poll ON async_job(status, run_after) WHERE status = 'pending';
```
Workers poll this table with `SELECT ... FOR UPDATE SKIP LOCKED` — a well-known Postgres pattern that gives you safe concurrent job processing without a separate broker.

---

## 4. Row-Level Security (Tenant Isolation)

Applied to every `org_id`-bearing table, e.g.:

```sql
ALTER TABLE conversation ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON conversation
    USING (org_id = current_setting('app.current_org_id')::UUID);
```

The application sets `app.current_org_id` at the start of each authenticated request (from the JWT claim), so even a bug in application-layer filtering can't leak another tenant's rows.

---

## 5. Indexing Strategy Summary

- Every `org_id` column indexed (tenant-scoped queries are the majority of traffic).
- `knowledge_chunk.embedding` uses an `ivfflat` index for approximate nearest-neighbor search — required for RAG to be fast at any real data volume.
- Foreign keys indexed on the "many" side for join performance.
- `async_job(status, run_after)` partial index so the polling query stays fast as the table grows.

---

## 6. Implementation Addendum — `identity` Service Actual Schema (added after Epic 1)

The original ERD above sketched `User` at a high level. Here's the schema as actually built and migrated, including fields added mid-implementation that weren't anticipated in the original design (Google OAuth support, specifically):

### `app_user` (final shape, after V1 + V5 migrations)
```sql
CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),              -- nullable (V5) — Google-only users have none
    google_id VARCHAR(255) UNIQUE,            -- added V5, for Google OAuth account linking
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_app_user_email ON app_user(email);
CREATE INDEX idx_app_user_google_id ON app_user(google_id);
```
**Why `password_hash` became nullable:** a user who signs up exclusively via Google never sets a password. Rather than a sentinel/dummy hash, `NULL` cleanly represents "no password auth method for this account" — checked via `AppUser.hasPassword()` before any password-based flow.

### `refresh_token`
```sql
CREATE TABLE refresh_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id),
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### `email_verification_token` and `password_reset_token`
Same shape as each other — single-use, hashed, expiring tokens:
```sql
CREATE TABLE email_verification_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id),
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- password_reset_token: identical structure, separate table
```

**Login behavior note (deviated from original SRS during Epic 1 — see Doc 02 FR-1.3):** login is blocked entirely with `403 EMAIL_NOT_VERIFIED` until the email is verified, rather than the originally-planned "allow login, just restrict widget publish." Password check happens *before* the verification check, so a wrong-password attempt on an unverified account returns `401 INVALID_CREDENTIALS`, not `403`, to avoid confirming account existence/state to an attacker who doesn't have valid credentials yet.

### `organization` / `membership` (built in Epic 2, matches original design)
```sql
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
    user_id UUID NOT NULL,   -- no FK to app_user by convention — see note below
    role VARCHAR(20) NOT NULL CHECK (role IN ('owner','admin','agent')),
    invited_at TIMESTAMPTZ,
    joined_at TIMESTAMPTZ,
    UNIQUE(org_id, user_id)
);
```
**No cross-service foreign key** on `membership.user_id` → `app_user.id`, even though both tables physically live in the same database in Phase 1 — deliberate, treating the service boundary as real even where physically it isn't yet. Referential integrity across this boundary is an application-level responsibility, enforced by `organization` service checking with `identity` (not yet built as an actual service call — currently, membership creation trusts the `userId` extracted from a verified JWT, which is safe since that ID can only come from identity's own token issuance).

## 7. Production/Client-Onboarding Checklist (schema-related)

When there's a real deployment target or paying client, revisit:
- [ ] Confirm `VECTOR(768)` dimension still matches whichever embedding model is live in prod (Doc 07 §3) — this is a breaking schema change if it ever needs to differ.
- [ ] Add proper `DOWN` migration strategy or backup-before-migrate step for prod deploys — Phase 1 has none, acceptable for a demo environment, not for real customer data.
- [ ] Revisit conversation/message data retention (Doc 02 NFR-9 flagged this as deferred) before onboarding any customer with real end-user chat data.
- [ ] Decide whether `membership.user_id` gets an actual cross-service integrity mechanism (event-based sync, or a real service call) once `organization` and `identity` might run on genuinely separate databases.

---

## 8. Next Steps

→ **Doc 05: API Design** (REST endpoints per service, request/response contracts)
