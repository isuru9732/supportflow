# 03 — System Architecture

**Status:** Draft v1.0
**References:** 01-Product-Vision.md, 02-SRS.md
**Last updated:** 2026-07-01

---

## 1. Architecture Style

**Modular services, REST-first, selective async.**

Rationale: full event-driven microservices (RabbitMQ between every service) is significant operational overhead for a solo developer at 5–8 hrs/week — retries, dead-letter queues, schema evolution, local debugging complexity all multiply. Instead:

- **Synchronous REST** between services for anything request/response in nature (auth checks, fetching org data, fetching conversation history).
- **Asynchronous (queue-based)** only where the operation is genuinely long-running or must not block a user-facing request:
  1. **Document ingestion** (upload → extract → chunk → embed) — can take seconds to minutes; must not block the upload response.
  2. **AI response generation** — LLM calls are the slowest part of the system; handled via a job so the WebSocket connection stays responsive and retries/timeouts are handled centrally.

This is still a legitimate "event-driven where it matters" architecture for portfolio purposes — it demonstrates judgment about *when* to use async, which is arguably more impressive than defaulting to a queue for everything.

---

## 2. C4 — Level 1: System Context

```
                        ┌─────────────────────────┐
                        │   Business Owner/Admin   │
                        │   (Org Owner, Agent)     │
                        └───────────┬──────────────┘
                                    │ uses (browser)
                                    ▼
                        ┌─────────────────────────┐
     Website Visitor    │                         │      LLM Provider
    ┌─────────────────► │   SupportFlow Platform  │◄────  (Gemini /
    │  (via widget)      │                         │       Ollama / OpenAI)
    │                    └───────────┬──────────────┘
    │                                │
    │                                ▼
    │                    ┌─────────────────────────┐
    └───────────────────►│   Email Provider         │
      (verification,     │   (transactional email)  │
       notifications)    └─────────────────────────┘
```

**Actors:**
- Business Owner/Admin/Agent — manages org, handles conversations (Dashboard app)
- Website Visitor — chats via embedded Widget on the business's own site
- LLM Provider — external AI API (swappable)
- Email Provider — transactional email (verification, invites, password reset)

---

## 3. C4 — Level 2: Container Diagram

```
┌────────────────┐     ┌────────────────┐
│  Dashboard      │     │  Chat Widget    │
│  (Next.js)      │     │  (embedded JS)  │
└────────┬────────┘     └────────┬────────┘
         │ HTTPS/REST            │ WebSocket + REST
         └───────────┬───────────┘
                      ▼
              ┌───────────────┐
              │  API Gateway   │  (routing, auth check, rate limiting)
              └───────┬────────┘
                      │
     ┌────────┬───────┼────────┬──────────┬───────────┐
     ▼        ▼        ▼        ▼          ▼           ▼
┌─────────┐┌────────┐┌───────┐┌─────────┐┌──────────┐┌──────────────┐
│Identity ││Org     ││Chat   ││Knowledge││AI        ││Notification  │
│Service  ││Service ││Service││Service  ││Service   ││Service        │
└────┬────┘└───┬────┘└───┬───┘└────┬────┘└────┬─────┘└──────┬───────┘
     │         │         │         │          │             │
     └─────────┴─────────┴────┬────┴──────────┴─────────────┘
                               ▼
                    ┌─────────────────────┐
                    │  PostgreSQL          │
                    │  (+ pgvector)         │
                    └─────────────────────┘
                               │
                    ┌─────────────────────┐
                    │  Redis                │
                    │  (sessions, cache,     │
                    │   pub/sub for WS)      │
                    └─────────────────────┘
                               │
                    ┌─────────────────────┐
                    │  Message Queue        │
                    │  (document ingestion,  │
                    │   AI response jobs)    │
                    └─────────────────────┘
```

**Service responsibilities:**

| Service | Owns | Talks to |
|---|---|---|
| **API Gateway** | Routing, JWT validation, rate limiting per API key | All services |
| **Identity Service** | Auth, users, roles, JWT issuing/refresh | Org Service (on registration) |
| **Organization Service** | Orgs, memberships, invites, settings | Identity Service |
| **Chat Service** | Conversations, messages, WebSocket connections, live queue | Knowledge Service (search), AI Service (via queue), Notification Service |
| **Knowledge Service** | Document upload, chunking, embeddings, similarity search | Postgres/pgvector, Queue (ingestion jobs) |
| **AI Service** | Provider abstraction, prompt construction, calling LLM | Knowledge Service (context), Queue (response jobs) |
| **Notification Service** | Email sending (verification, invites, resets) | Email Provider |

---

## 4. C4 — Level 3: Component Diagram (Chat Service example)

```
┌─────────────────────────────────────────────┐
│               Chat Service                    │
│                                                 │
│  ┌───────────────┐   ┌──────────────────┐    │
│  │ WebSocket       │   │ Conversation      │    │
│  │ Connection Mgr  │──►│ Controller        │    │
│  └───────────────┘   └─────────┬─────────┘    │
│                                  │               │
│                       ┌──────────▼─────────┐    │
│                       │ Message Service     │    │
│                       └──────────┬─────────┘    │
│                                  │               │
│              ┌───────────────────┼───────────┐   │
│              ▼                   ▼           ▼   │
│      ┌──────────────┐  ┌────────────────┐ ┌────────────┐
│      │ AI Response   │  │ Human Takeover │ │ Message    │
│      │ Job Publisher │  │ Handler        │ │ Repository │
│      └──────────────┘  └────────────────┘ └────────────┘
└─────────────────────────────────────────────┘
```

This level will be repeated for each service as we get closer to implementation — not needed for all seven services up front.

---

## 5. Key Sequence: Visitor Asks a Question (AI-handled)

```
Visitor        Widget      Gateway    Chat Svc    Queue      AI Svc      Knowledge Svc     LLM
  │              │            │           │          │           │              │           │
  │──message────►│            │           │          │           │              │           │
  │              │──WS send──►│           │          │           │              │           │
  │              │            │──route───►│          │           │              │           │
  │              │            │           │─save msg │           │              │           │
  │              │            │           │─publish─►│           │              │           │
  │              │            │           │          │──consume─►│              │           │
  │              │            │           │          │           │──similarity──►│           │
  │              │            │           │          │           │◄──chunks──────│           │
  │              │            │           │          │           │──prompt+ctx──────────────►│
  │              │            │           │          │           │◄──answer───────────────────│
  │              │            │           │◄─publish result──────│              │           │
  │              │◄──WS msg───│◄──────────│          │           │              │           │
  │◄──answer─────│            │           │          │           │              │           │
```

---

## 6. Deployment View (Phase 1)

```
Development:
  Docker Compose → Next.js + all Spring services + Postgres + Redis + RabbitMQ + Ollama

Production (demo):
  Vercel (Dashboard + Widget CDN)
        │
        ▼
  Railway/Render (Spring Boot services, containerized)
        │
        ▼
  Neon (PostgreSQL + pgvector)
  Upstash (Redis)
  CloudAMQP free tier or Redis-based queue (avoid separate RabbitMQ host cost — see note below)
        │
        ▼
  Gemini API (default) / Ollama (local fallback, dev only)
```

**Note on the queue:** Running a separate RabbitMQ instance adds another free-tier account/service to manage. For Phase 1, consider using **Redis Streams or a simple Postgres-backed job table** instead of RabbitMQ — same async pattern, one less moving part to operate on a free tier. This can be swapped for RabbitMQ later if the portfolio story specifically needs to demonstrate message broker experience. Decision point, not fixed — revisit in Doc 06 (Backend Architecture) if you want RabbitMQ specifically for the resume line.

---

## 7. Implementation Addendum (added during Epic 2 — supersedes part of §3's assumption)

Doc 06 originally assumed the **Gateway** would extract `orgId` from the JWT and forward it internally to services. In practice, Epic 0's Gateway is still a routing skeleton (JWT validation not yet built out), so Epic 2 needed org-context resolution *before* the Gateway was ready for it. The actual interim implementation, now live in `organization` service:

- Each service **verifies the JWT itself** using a shared `JwtVerifier` (lives in the `common` module, `com.supportflow.common.security`), not the Gateway.
- Org context is resolved per-request via an **`X-Org-Id` header**, checked against the `membership` table (does this user actually belong to this org?) before any org-scoped query runs.
- A `OrgContextInterceptor` (Spring `HandlerInterceptor`) does this check and then runs `SET LOCAL app.current_org_id` for that request's transaction, wiring into the Postgres RLS policies from Doc 04 §4 / Doc 06 §3 exactly as originally designed — only *where* this happens moved (per-service now, Gateway later).

**This is a deliberate, temporary decision, not a workaround being hidden.** Nothing in the services themselves needs to change when the Gateway is eventually built out to do this centrally — the membership-check-then-set-session-variable logic can move wholesale into the Gateway later, and services simply start trusting a Gateway-forwarded header instead of validating the JWT themselves. Tracked as a real task, not forgotten: see Doc 10 Epic 0 remainder.

**Every service needing JWT verification must:**
1. Add `@ComponentScan(basePackages = {"com.supportflow.<service>", "com.supportflow.common"})` to its main application class (see Doc 09 for why — `common`'s `@Component` classes aren't auto-scanned otherwise).
2. Copy `identity`'s RSA **public** key only (never the private key) into `src/main/resources/keys/public_key.pem`.

---

## 8. Next Steps

→ **Doc 04: Database Design** (ERD + table definitions, building on the service boundaries above)
