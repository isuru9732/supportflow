# SupportFlow AI

AI-powered customer support platform — embeddable chat widget backed by a per-tenant RAG knowledge base, with human takeover and analytics.

Full planning documentation lives in [`/docs`](./docs) — start with `01-Product-Vision.md` for context, `03-System-Architecture.md` for the big picture, and `10-Agile-Backlog.md` for the current task list.

## Local Development Setup

1. **Prerequisites:** Docker Desktop, Java 17+, Maven, Node.js 20+, Git
2. **Clone and configure:**
   ```bash
   git clone <your-repo-url>
   cd supportflow
   cp .env.example .env
   # fill in .env — for pure local dev, defaults + AI_PROVIDER=ollama work with no API keys
   ```
3. **Start infra:**
   ```bash
   docker compose up -d postgres redis ollama
   ```
4. **Pull a local model for Ollama** (first time only):
   ```bash
   docker exec -it supportflow-ollama ollama pull llama3
   docker exec -it supportflow-ollama ollama pull nomic-embed-text
   ```
5. Backend services and the dashboard are added incrementally per the backlog (`docs/10-Agile-Backlog.md`, Epic 0 onward) — each gets its own setup instructions as it's scaffolded.

## Repo Structure

```
supportflow/
├── apps/dashboard/       Next.js dashboard (org admins, agents)
├── services/
│   ├── gateway/           API gateway, routing, JWT validation
│   ├── identity/           Auth, users, roles
│   ├── organization/       Orgs, memberships, invites
│   ├── chat/               Conversations, messages, WebSocket
│   ├── knowledge/          Document upload, chunking, embeddings, search
│   ├── ai/                 Provider abstraction, RAG pipeline
│   └── notification/       Email sending
├── widget/                 Embeddable chat widget (standalone JS)
├── docs/                   Full planning documentation (Docs 01–10)
├── docker-compose.yml
└── .env.example
```

## Architecture at a Glance

- **Multi-tenancy:** shared schema + `tenant_id` + Postgres Row-Level Security (Doc 04, 06)
- **AI:** swappable provider (Gemini / Ollama / OpenAI) behind a common interface, RAG over per-org knowledge base (Doc 07)
- **Async:** lightweight Postgres-backed job table instead of a separate message broker (Doc 03 §6, Doc 04 §3)

See `/docs` for full detail on every decision and why it was made.
