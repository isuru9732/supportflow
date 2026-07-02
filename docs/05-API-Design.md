# 05 — API Design

**Status:** Draft v1.0
**References:** 02-SRS.md, 03-System-Architecture.md, 04-Database-Design.md
**Last updated:** 2026-07-01

---

## 1. Conventions

- **Base path:** `/api/v1`
- **Auth:** `Authorization: Bearer <JWT>` for dashboard/agent endpoints. Widget endpoints use `X-Api-Key: <org api key>` instead (no user JWT — visitors are anonymous).
- **Content type:** `application/json` except file upload (`multipart/form-data`).
- **Tenant scoping:** every authenticated request resolves `org_id` from the JWT/API key — never accepted as a client-supplied parameter, to prevent tenant-spoofing.

### Request envelope

All POST/PATCH/PUT bodies wrap their payload in `data`:
```json
{
  "data": { "email": "owner@garage.com", "password": "••••••••" }
}
```

### Response envelope

Every 2xx response uses the same shape, whether `data` is a single object or a list:
```json
{
  "data": {},
  "meta": {},
  "pagination": {}
}
```

- **`data`** — the payload. Object for single-resource responses, array for list responses. Always present on success.
- **`meta`** — optional, response-specific context (e.g., `{ "requestId": "..." }`). Omitted when there's nothing to report.
- **`pagination`** — only present on list endpoints:
```json
"pagination": {
  "cursor": "eyJpZCI6...",
  "nextCursor": "eyJpZCI6...",
  "limit": 20,
  "hasMore": true
}
```

### Error format

Errors do **not** use the `data` envelope — kept as a distinct top-level shape so clients can branch on status code without inspecting body structure first:
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Email is already registered",
    "field": "email"
  }
}
```

---

## 2. Identity Service

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/auth/register` | Create account (email/password) | none |
| POST | `/auth/login` | Login, returns access + refresh token | none |
| POST | `/auth/google` | Google OAuth callback → tokens | none |
| POST | `/auth/refresh` | Exchange refresh token for new access token | refresh token |
| POST | `/auth/logout` | Revoke refresh token | JWT |
| POST | `/auth/verify-email` | Confirm email via token | none |
| POST | `/auth/forgot-password` | Trigger reset email | none |
| POST | `/auth/reset-password` | Set new password via token | none |
| GET | `/users/me` | Current user profile | JWT |

**Example: `POST /auth/register`**
```json
// Request
{
  "data": { "email": "owner@garage.com", "password": "••••••••" }
}

// Response 201
{
  "data": {
    "userId": "uuid",
    "email": "owner@garage.com",
    "status": "pending_verification"
  }
}
```

---

## 3. Organization Service

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/orgs` | Create organization | JWT |
| GET | `/orgs/current` | Get current org (resolved from JWT membership) | JWT |
| PATCH | `/orgs/current` | Update org settings (name, branding, timezone) | JWT (owner/admin) |
| GET | `/orgs/current/members` | List members | JWT |
| POST | `/orgs/current/members/invite` | Invite by email | JWT (owner/admin) |
| PATCH | `/orgs/current/members/{userId}` | Change role | JWT (owner) |
| DELETE | `/orgs/current/members/{userId}` | Remove member | JWT (owner) |
| POST | `/orgs/current/api-keys` | Generate widget API key | JWT (owner/admin) |
| DELETE | `/orgs/current/api-keys/{keyId}` | Revoke key | JWT (owner/admin) |

---

## 4. Knowledge Service

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/knowledge/documents` | Upload document (multipart) | JWT (admin) |
| GET | `/knowledge/documents` | List documents + processing status | JWT |
| GET | `/knowledge/documents/{id}` | Get document detail | JWT |
| DELETE | `/knowledge/documents/{id}` | Delete document + its chunks | JWT (admin) |
| POST | `/knowledge/faq` | Add manual FAQ entry | JWT (admin) |
| GET | `/knowledge/faq` | List FAQ entries | JWT |
| DELETE | `/knowledge/faq/{id}` | Delete FAQ entry | JWT (admin) |
| POST | `/knowledge/search` | **Internal only** — similarity search, called by AI Service | service-to-service token |

**Example: `POST /knowledge/documents` response**
```json
{
  "data": {
    "id": "uuid",
    "filename": "pricing-faq.pdf",
    "status": "processing",
    "uploadedAt": "2026-07-01T10:00:00Z"
  }
}
```

**Example: `GET /knowledge/documents` (list, paginated)**
```json
{
  "data": [
    { "id": "uuid1", "filename": "pricing-faq.pdf", "status": "ready" },
    { "id": "uuid2", "filename": "policies.docx", "status": "processing" }
  ],
  "pagination": {
    "cursor": null,
    "nextCursor": "eyJpZCI6InV1aWQyIn0=",
    "limit": 20,
    "hasMore": true
  }
}
```

---

## 5. Chat Service

### Dashboard/Agent-facing (JWT)
| Method | Path | Description |
|---|---|---|
| GET | `/conversations` | List conversations (filter by status/mode) |
| GET | `/conversations/{id}` | Get conversation + messages |
| POST | `/conversations/{id}/claim` | Agent takes over from AI |
| POST | `/conversations/{id}/release` | Hand back to AI |
| POST | `/conversations/{id}/resolve` | Mark resolved |
| POST | `/conversations/{id}/messages` | Agent sends a message |

### Widget-facing (API key, anonymous visitor)
| Method | Path | Description |
|---|---|---|
| POST | `/widget/conversations` | Start a new conversation, returns `visitorSessionToken` |
| POST | `/widget/conversations/{id}/messages` | Visitor sends message |
| GET | `/widget/conversations/{id}/messages` | Fetch history (on reload) |

### WebSocket
| Endpoint | Description |
|---|---|
| `wss://.../ws/conversations/{id}` | Real-time message stream, both dashboard and widget connect here (auth via JWT or session token in connection handshake) |

---

## 6. AI Service (internal — not exposed publicly)

| Method | Path | Description |
|---|---|---|
| POST | `/internal/ai/respond` | Given conversation context, generate AI answer (called by async_job worker, not directly by clients) |

**Request:**
```json
{
  "data": {
    "orgId": "uuid",
    "conversationId": "uuid",
    "visitorMessage": "How much is a wheel alignment?",
    "providerOverride": null
  }
}
```
**Response:**
```json
{
  "data": {
    "answer": "Our standard wheel alignment starts at...",
    "sourceChunkIds": ["uuid1", "uuid2"],
    "confidence": 0.82,
    "fallbackTriggered": false
  },
  "meta": {
    "providerUsed": "gemini",
    "latencyMs": 1240
  }
}
```

---

## 7. Dashboard/Analytics

| Method | Path | Description |
|---|---|---|
| GET | `/analytics/summary` | Today's conversations, AI vs human resolved counts |
| GET | `/analytics/top-questions` | Most-asked questions (derived from message logs) |
| GET | `/analytics/response-time` | Average response time |

---

## 8. Rate Limiting

Applied at the Gateway, keyed by:
- **JWT endpoints:** per user, generous limits (dashboard usage patterns)
- **Widget endpoints:** per API key, stricter limits to control AI cost — this is the main cost-control lever for the free-tier LLM quota (ties back to NFR-5 in the SRS)

---

## 9. List Endpoints Reference

All `GET` endpoints returning collections follow the paginated list envelope shown in §4. This applies to: `GET /orgs/current/members`, `GET /knowledge/documents`, `GET /knowledge/faq`, `GET /conversations`, `GET /conversations/{id}/messages` (widget history), `GET /analytics/top-questions`. Single-resource `GET`s (e.g. `GET /users/me`, `GET /orgs/current`) return `data` as an object with no `pagination` key.

## 10. Next Steps

→ **Doc 06: Authentication & Multi-Tenancy Deep Dive** (JWT structure, token rotation, RLS session-variable wiring, invite flow detail)
