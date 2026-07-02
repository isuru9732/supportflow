# 09 — Deployment & CI/CD Architecture

**Status:** Draft v1.0
**References:** 01-Product-Vision.md (§9 stack), 03-System-Architecture.md (§6 deployment view), 08-Security-Architecture.md
**Last updated:** 2026-07-01

---

## 1. Environments

| Environment | Purpose | Where it runs |
|---|---|---|
| **Local** | Day-to-day development | Docker Compose on your machine |
| **Staging/Demo** | What recruiters/prospects actually see | Vercel + Railway/Render + Neon + Upstash (free tiers) |
| **Production** | Deferred until there's a real paying customer — no separate prod environment built yet | — |

**Rationale for skipping a separate prod environment now:** running staging *and* prod on free tiers doubles infra to manage for no current benefit — there are no real customers yet. Staging/Demo effectively *is* prod until that changes. This is called out explicitly so it's a decision, not an oversight, and revisited in Doc 20 (Roadmap) as a trigger condition ("first paying customer → stand up proper prod").

---

## 2. Local Development — Docker Compose

```yaml
# docker-compose.yml (structure, not exhaustive)
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: supportflow
    ports: ["5432:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  ollama:
    image: ollama/ollama
    ports: ["11434:11434"]
    volumes: ["ollama_models:/root/.ollama"]

  gateway:
    build: ./services/gateway
    ports: ["8080:8080"]
    depends_on: [identity-service, org-service, chat-service]

  identity-service:
    build: ./services/identity
    depends_on: [postgres]
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/supportflow

  # org-service, chat-service, knowledge-service, ai-service,
  # notification-service follow the same pattern

  dashboard:
    build: ./apps/dashboard
    ports: ["3000:3000"]
    depends_on: [gateway]

volumes:
  pgdata:
  ollama_models:
```

One `docker-compose up` brings up the entire stack locally, including Ollama for zero-cost AI development — matching the "$0 local development" goal from the original plan.

---

## 3. Repository Structure

**Decision: monorepo.** For a solo developer, a monorepo (one repo, multiple services/apps in subfolders) is significantly easier to manage than coordinating multiple repos — one CI pipeline, one place to search code, atomic commits across service boundaries when an API contract changes on both sides at once.

```
supportflow/
├── apps/
│   └── dashboard/          (Next.js)
├── services/
│   ├── gateway/
│   ├── identity/
│   ├── organization/
│   ├── chat/
│   ├── knowledge/
│   ├── ai/
│   └── notification/
├── widget/                 (embeddable JS, built separately, served via CDN)
├── docs/                   (this documentation set)
├── docker-compose.yml
└── .github/workflows/
```

---

## 4. CI/CD Pipeline (GitHub Actions)

```
On push to any branch:
  ┌─────────────────────────────────────┐
  │ 1. Lint (per changed service/app)     │
  │ 2. Unit tests (per changed service)   │
  │ 3. Build (verify compilation)         │
  └─────────────────────────────────────┘

On merge to main:
  ┌─────────────────────────────────────┐
  │ 1. All of the above                   │
  │ 2. Integration tests (Docker Compose  │
  │    spun up in CI, run against real    │
  │    Postgres/Redis)                    │
  │ 3. Build Docker images                │
  │ 4. Push images to registry (GHCR)     │
  │ 5. Deploy:                            │
  │    - Dashboard → Vercel (auto via     │
  │      GitHub integration, no extra     │
  │      workflow step needed)            │
  │    - Backend services → Railway/      │
  │      Render (triggered via their      │
  │      GitHub integration or deploy     │
  │      hook)                            │
  └─────────────────────────────────────┘
```

**Path-based triggers:** since it's a monorepo, use `paths:` filters in GitHub Actions so a change to `services/chat` doesn't rebuild/redeploy `services/knowledge` unnecessarily — keeps CI fast and free-tier CI minutes from being wasted.

---

## 5. Database Migrations

- **Tool:** Flyway (pairs naturally with Spring Boot).
- Migrations live per-service (`services/identity/src/main/resources/db/migration/`) since each service technically owns its own tables even though they share one physical database in Phase 1 (shared schema per Doc 04) — this keeps a future move to separate databases per service (if ever needed) from being a rewrite.
- Migrations run automatically on service startup in dev; run as an explicit CI step before deploy in staging (avoids a service crash-looping on a bad migration reaching prod-like environment un-reviewed).

---

## 6. Configuration Management

- `application.yml` per service with environment-specific profiles (`local`, `staging`).
- Secrets injected via environment variables at deploy time (per Doc 08 §2) — never baked into Docker images.
- The `AI_PROVIDER` config from Doc 07 §5 is set per environment: `ollama` locally, `gemini` in staging/demo.

---

## 7. Rollback Strategy

- Railway/Render both support rolling back to a previous deploy via their dashboard — sufficient for Phase 1 (no need to build custom blue-green deployment tooling for a project at this stage).
- Database migrations should be written to be backward-compatible where possible (additive changes preferred over destructive ones) so a code rollback doesn't require a matching DB rollback.

---

## 8. What's Explicitly Deferred

- Kubernetes / container orchestration — unnecessary complexity at this scale; Railway/Render handle container orchestration adequately for Phase 1 traffic.
- Blue-green or canary deployments — no traffic volume that justifies this yet.
- Infrastructure-as-code (Terraform) — platform dashboards are sufficient for the current number of resources; revisit if the stack grows significantly.

---

## 9. Next Steps

→ **Doc 10: Agile Backlog** (Epics → Features → Stories → Tasks, sized for 5–8 hrs/week — this is what turns everything so far into an actual week-by-week build plan)
