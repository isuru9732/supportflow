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

## 7. Next Steps

→ **Doc 04: Database Design** (ERD + table definitions, building on the service boundaries above)
