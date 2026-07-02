# 02 — Software Requirements Specification (SRS)

**Status:** Draft v1.0
**References:** 01-Product-Vision.md
**Last updated:** 2026-07-01

---

## 1. Purpose

This document defines the functional and non-functional requirements for Phase 1 (MVP) of the platform, as scoped in Doc 01. Every user story here maps to something that will appear in the Agile Backlog (Doc 11).

## 2. Actors

- **Org Owner** — first user of a tenant, full permissions
- **Org Admin** — invited by Owner, can manage settings/knowledge/agents but not billing/delete-org
- **Agent** — handles live conversations, no settings access
- **Visitor** — anonymous end customer using the chat widget
- **System (AI)** — acts as an actor for RAG-driven responses

## 3. Functional Requirements

### 3.1 Authentication & Account Management

| ID | Requirement | Priority |
|---|---|---|
| FR-1.1 | User can register with email + password | Must |
| FR-1.2 | User can log in with Google OAuth | Must |
| FR-1.3 | User receives email verification link on signup; account is limited (no widget publish) until verified | Must |
| FR-1.4 | User can request password reset via email | Must |
| FR-1.5 | Passwords are hashed (bcrypt/argon2); never logged or stored in plaintext | Must |
| FR-1.6 | JWT access token (short-lived) + refresh token (long-lived, rotated) issued on login | Must |
| FR-1.7 | MFA (TOTP) | Deferred to Phase 2 |

**User story example:**
> As a new user, I want to sign up with my email so that I can create a workspace for my business.
> **Acceptance criteria:** Given valid email/password, when I submit the signup form, then an account is created in `pending_verification` state and a verification email is sent within 30 seconds.

### 3.2 Organization (Tenant) Management

| ID | Requirement | Priority |
|---|---|---|
| FR-2.1 | On first login, user is prompted to create an organization (name, optional logo) | Must |
| FR-2.2 | Creating an org assigns the creating user the `Owner` role | Must |
| FR-2.3 | Owner/Admin can invite users by email; invited user gets `Agent` role by default, adjustable | Must |
| FR-2.4 | Owner can change a member's role (Admin/Agent) | Must |
| FR-2.5 | Owner can remove a member | Must |
| FR-2.6 | Each org has an isolated data boundary enforced at the DB layer (see Doc 07) | Must |
| FR-2.7 | Org has a settings page: name, timezone, widget branding color | Should |

### 3.3 Knowledge Base

| ID | Requirement | Priority |
|---|---|---|
| FR-3.1 | Admin can upload PDF, DOCX, or TXT files as knowledge sources | Must |
| FR-3.2 | Uploaded documents are chunked and embedded automatically (async job) | Must |
| FR-3.3 | Admin can see processing status per document (queued/processing/ready/failed) | Must |
| FR-3.4 | Admin can delete a document, which removes its chunks/embeddings | Must |
| FR-3.5 | Admin can manually add FAQ entries (question/answer pairs) without file upload | Should |
| FR-3.6 | Max file size and supported types are enforced with clear error messages | Must |

**User story example:**
> As an Org Admin, I want to upload our FAQ PDF so that the AI can answer customer questions using our real policies.
> **Acceptance criteria:** Given a valid PDF under the size limit, when uploaded, then the document appears with status `processing`, transitions to `ready` within a reasonable time, and its content becomes searchable by the AI.

### 3.4 Chat Widget

| ID | Requirement | Priority |
|---|---|---|
| FR-4.1 | Each org gets a unique embeddable script + API key | Must |
| FR-4.2 | Widget renders as a floating bubble, opens into a chat window | Must |
| FR-4.3 | Widget is mobile-responsive | Must |
| FR-4.4 | Widget connects via WebSocket for real-time messaging | Must |
| FR-4.5 | Widget shows typing indicator and AI "thinking" state | Should |
| FR-4.6 | Widget branding (color, greeting message) configurable per org | Should |
| FR-4.7 | Visitor conversation persists across page reloads within the same session (browser storage of a session token) | Should |

### 3.5 AI Chat (RAG)

| ID | Requirement | Priority |
|---|---|---|
| FR-5.1 | Visitor message triggers similarity search against that org's embedded knowledge chunks only | Must |
| FR-5.2 | Retrieved context + visitor question sent to configured LLM provider to generate an answer | Must |
| FR-5.3 | If no relevant knowledge is found above a similarity threshold, AI responds with a graceful fallback (offers human handoff) rather than hallucinating | Must |
| FR-5.4 | AI provider is swappable via configuration (Gemini / Ollama / OpenAI) without code changes | Must |
| FR-5.5 | Every AI response is logged with the source chunks used, for later review/debugging | Should |

### 3.6 Human Takeover / Live Chat

| ID | Requirement | Priority |
|---|---|---|
| FR-6.1 | Visitor can request a human at any point during the chat | Must |
| FR-6.2 | Agent sees a live queue of conversations needing attention | Must |
| FR-6.3 | Agent can claim a conversation, which pauses AI auto-response for that conversation | Must |
| FR-6.4 | Visitor sees a system message when a human joins (e.g., "Agent Sarah joined") | Must |
| FR-6.5 | Agent can hand a conversation back to AI or mark it resolved | Should |

### 3.7 Dashboard & Analytics

| ID | Requirement | Priority |
|---|---|---|
| FR-7.1 | Dashboard shows: conversations today, total visitors, AI-resolved vs human-resolved count | Must |
| FR-7.2 | Dashboard shows a list of most-asked questions (derived from logged AI queries) | Should |
| FR-7.3 | Dashboard shows average response time | Should |
| FR-7.4 | All dashboard data is scoped to the logged-in user's org only | Must |

## 4. Non-Functional Requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-1 | Security | All tenant data isolated at the database layer (RLS); no cross-tenant data leakage under any query path |
| NFR-2 | Security | All API endpoints require authentication except the public widget message endpoint (which is scoped by API key) |
| NFR-3 | Performance | AI response returned to visitor within 5 seconds for 95% of queries (excluding cold-start on free-tier hosting) |
| NFR-4 | Availability | Acceptable to sleep/cold-start on free tier for demo purposes; not a production SLA target in Phase 1 |
| NFR-5 | Cost | Entire stack must run within free-tier limits for demo purposes |
| NFR-6 | Extensibility | AI provider, and eventually tenancy model, must be swappable without rewriting business logic |
| NFR-7 | Compatibility | Widget must work on modern evergreen browsers; no IE support needed |
| NFR-8 | Observability | Key actions (signup, upload, AI response, takeover) are logged for debugging |
| NFR-9 | Data retention | Conversation history retained indefinitely in Phase 1 (revisit retention policy before real customers) |

## 5. Out-of-Scope Confirmation

Reiterating Doc 01 §6 — ticketing, billing enforcement, channel integrations (WhatsApp/Slack/email), sentiment analysis, and voice are **not** part of this SRS and have no requirements defined here.

## 6. Open Questions (to resolve before Doc 05/06)

- Exact similarity threshold for "no relevant knowledge found" fallback — will be tuned empirically, not fixed in requirements.
- Session persistence mechanism for visitor widget (localStorage token vs cookie) — decide in Frontend Architecture doc.

## 7. Next Steps

→ Doc 03: (merged into this SRS — functional/non-functional combined for Phase 1 to avoid redundant documents)
→ **Doc 04: System Architecture (C4 diagrams)**
