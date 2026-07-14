# 06 — Authentication & Multi-Tenancy Deep Dive

**Status:** Draft v1.0
**References:** 02-SRS.md, 03-System-Architecture.md, 04-Database-Design.md, 05-API-Design.md
**Last updated:** 2026-07-01

---

## 1. JWT Structure

### Access token (short-lived, 15 min)
```json
{
  "sub": "user-uuid",
  "orgId": "org-uuid",
  "role": "admin",
  "email": "owner@garage.com",
  "iat": 1751360000,
  "exp": 1751360900,
  "type": "access"
}
```

### Refresh token (long-lived, 30 days, rotated on use)
```json
{
  "sub": "user-uuid",
  "tokenId": "refresh-token-row-uuid",
  "type": "refresh"
}
```

**Why `orgId` is embedded in the access token, not looked up per-request:** avoids an extra DB round-trip on every authenticated call to resolve which org the user is acting as. Trade-off: if a user's role changes mid-session, the change doesn't take effect until the token refreshes (max 15 min staleness) — acceptable for Phase 1. Flagged here so it's a conscious decision, not an oversight.

**Multi-org users:** a user can belong to multiple orgs (multiple `membership` rows). The access token is scoped to *one* org at a time — the one currently selected in the dashboard. Switching orgs re-issues a token via `POST /auth/switch-org` (add to Doc 05 API list if multi-org support is confirmed needed — not in original SRS scope, flagging as an open question rather than silently adding it).

---

## 2. Token Lifecycle

```
Login ──► Access Token (15 min) + Refresh Token (30 days, stored hashed in DB)
             │
             ▼ (expires)
     Client calls /auth/refresh with refresh token
             │
             ▼
     Server validates refresh_token row (not revoked, not expired)
             │
             ▼
     Issues NEW access token + NEW refresh token
     Old refresh token row marked used/revoked (rotation)
```

**Rotation matters here:** if a refresh token is ever stolen and used by an attacker, the legitimate user's next refresh attempt will fail (token already rotated), which is a detectable signal — the app can force a full re-login and flag the session as compromised. Storing only a *hash* of the refresh token (matching the `token_hash` column from Doc 04) means a DB leak alone doesn't expose usable tokens.

---

## 3. Row-Level Security Wiring (how `app.current_org_id` actually gets set)

This connects the JWT to the Postgres RLS policies defined in Doc 04 §4. The flow per request:

```
1. Gateway validates JWT signature + expiry
2. Gateway extracts orgId claim
3. Gateway forwards request to service with orgId in a trusted internal header
   (X-Internal-Org-Id — only the gateway can set this; services reject if it
   arrives from outside the internal network)
4. Service, at the start of the DB transaction, runs:
       SET LOCAL app.current_org_id = '<orgId>';
5. All subsequent queries in that transaction are filtered by RLS policies
   automatically — even a forgotten WHERE org_id = ? in application code
   cannot leak cross-tenant rows.
```

**`SET LOCAL` (not `SET`)** — scopes the setting to the current transaction only, so connection pooling can't leak one tenant's context into the next request that reuses the same pooled connection. This detail matters a lot in practice and is a common source of real-world multi-tenant bugs if missed.

**Implementation note (Epic 2):** steps 1–3 above describe the *intended end state* once the Gateway is fully built. In the actual Epic 2 implementation, the Gateway isn't there yet, so each service (starting with `organization`) validates the JWT itself via a shared `JwtVerifier` (in the `common` module) and resolves org context from an `X-Org-Id` header + a membership-table check, rather than trusting a Gateway-forwarded claim. Full detail in Doc 03 §7. The RLS wiring itself (`SET LOCAL app.current_org_id`) is unchanged — only *what validates the request and extracts the org id* moved, temporarily, into each service.

---

## 4. Invite Flow (including edge cases)

```
Owner/Admin invites "agent@garage.com"
        │
        ▼
Does a User already exist with this email?
   │                           │
  YES                          NO
   │                           │
   ▼                           ▼
Create Membership          Create User (status: invited,
row (status: invited)      no password set yet)
   │                           │
   ▼                           ▼
Send "You've been added    Send "You've been invited —
to {Org}" email             set your password" email
   │                           │
   └───────────┬───────────────┘
               ▼
     Invitee clicks link → sets/confirms password
     (if new user) → Membership.joined_at set
```

**Edge cases covered:**
- Inviting an email already registered under a *different* org — fine, a user can have multiple memberships.
- Inviting the same email twice before they accept — second invite should update the existing pending Membership row's `invited_at`, not create a duplicate (the `UNIQUE(org_id, user_id)` constraint from Doc 04 only works once the User row exists; for the not-yet-registered case, dedupe on `(org_id, invited_email)` before user creation).
- Revoking an invite before acceptance — delete the Membership row if `joined_at IS NULL`.

---

## 5. Google OAuth Flow

**As actually implemented (differs from the original code-exchange sketch below):** the frontend uses Google Identity Services' client-side sign-in to get a signed **ID token** directly from Google, then sends just that token to `POST /auth/google`. The backend verifies the token's signature against Google's public keys (via `GoogleIdTokenVerifier`, with `setAudience` pinned to our client ID) — no authorization-code exchange, no client secret needed on our backend at all. Simpler and standard for SPA + separate-backend architectures like ours.

```
Browser → Google Identity Services JS → user signs in → Google returns
          a signed ID token directly to the browser
Browser → POST /auth/google { idToken }
Backend → GoogleIdTokenVerifier.verify(idToken) — checks signature +
           audience (must match our GOOGLE_OAUTH_CLIENT_ID)
Backend → extract email, email_verified, sub (Google's stable user id)
Backend → find by google_id, else find-or-create by email:
   - Existing password-based account with this email → link Google
     (set google_id on the existing row) rather than creating a duplicate
   - No existing account → create new user with password_hash = NULL
     (see Doc 04 §6), email_verified = true (Google's verification trusted)
Backend → issues access + refresh token as normal
```

**Google Cloud setup required** (one-time, per environment): OAuth consent screen configured, OAuth client ID (Web application type) created, authorized JavaScript origins added per environment (`http://localhost:5500` for local test harness; the real dashboard domain once deployed). Client ID goes in `GOOGLE_OAUTH_CLIENT_ID` env var — **no client secret needed** for this flow, since verification is signature-based, not code-exchange-based.

**Production reminder:** the authorized JavaScript origins list in Google Cloud Console must be updated to include the real staging/demo domain (Doc 09 §1) before Google sign-in will work anywhere other than localhost — easy to forget since local dev works fine without it.

---

## 6. Password & Session Security Checklist

- Passwords hashed with **argon2id** (preferred over bcrypt for new systems; both acceptable, argon2id is the more current recommendation).
- Refresh tokens stored as SHA-256 hashes, never plaintext.
- Rate limiting on `/auth/login` and `/auth/forgot-password` specifically (brute-force and enumeration protection) — separate, stricter limit than general API rate limiting from Doc 05 §8.
- Email verification and password reset tokens are single-use (`used_at` column checked) and short-lived (verification: 24h, reset: 1h).
- No user enumeration: `/auth/forgot-password` returns the same success response whether or not the email exists.

---

## 7. Open Questions Resolved / Carried Forward

| Question | Resolution |
|---|---|
| Multi-org user support | Not in original SRS — flagged as **out of scope for Phase 1**. Membership table supports it later without migration. |
| Session persistence for widget visitors | `visitorSessionToken` (from Doc 05 `POST /widget/conversations`) stored in browser localStorage by the widget script, sent back on reconnect. Not a JWT — just an opaque token mapping to a `visitor_id` string on the Conversation table. |

---

## 8. Next Steps

→ **Doc 07: AI / RAG Architecture** (embedding pipeline detail, prompt construction, provider abstraction interface)
