# 10 — Agile Backlog

**Status:** Draft v1.0
**References:** All prior docs (01–09)
**Last updated:** 2026-07-01
**Sizing basis:** ~6 hrs/week average, in ~45–90 min evening sessions (4–6 sessions/week)

---

## 1. How to Read This Backlog

Every **Task** is sized to fit in one evening session (45–90 min). If a task feels like it'll spill past that, it's split further — the goal is that you can open this doc on a random weeknight, pick the next unchecked task, and know exactly what "done" looks like without re-deriving context from the architecture docs.

**Estimate legend:** hrs = realistic solo-dev hours including debugging, not just "typing time."

---

## 2. Epic Summary & Milestones

| # | Epic | Est. hrs | Depends on |
|---|---|---|---|
| E0 | Infrastructure & Project Setup | 10–14 | — |
| E1 | Authentication & Identity | 16–20 | E0 |
| E2 | Organization & Team Management | 10–14 | E1 |
| E3 | Knowledge Base | 18–24 | E0, E2 |
| E4 | Chat Widget (frontend) | 14–18 | E2 |
| E5 | AI / RAG Chat | 20–26 | E3, E4 |
| E6 | Human Takeover / Live Chat | 14–18 | E4, E5 |
| E7 | Dashboard & Analytics | 12–16 | E5, E6 |
| E8 | Deployment, Polish, Demo-Readiness | 12–16 | all above |
| **Total** | | **~126–166 hrs** | |

**At 6 hrs/week average: ~21–28 weeks (≈5–7 months).** This is an honest estimate, not a motivational one — full-stack multi-service builds take real time even lean. Milestones below let you see visible progress well before the full timeline, which matters more than the total for staying motivated.

---

## 3. Milestone Plan (visible progress checkpoints)

| Milestone | Epics complete | What you can demo |
|---|---|---|
| **M1** (~wk 5) | E0, E1 | Register, log in, JWT auth working end to end |
| **M2** (~wk 8) | E2 | Create org, invite a teammate, roles working |
| **M3** (~wk 13) | E3 | Upload a PDF, see it chunked/embedded, search works via API |
| **M4** (~wk 17) | E4 | Widget embeds on a test HTML page, sends/receives messages live |
| **M5** (~wk 22) | E5 | Full RAG loop — ask a question, get an AI answer from real knowledge base |
| **M6** (~wk 25) | E6 | Agent can see live queue, claim, and take over a conversation |
| **M7** (~wk 27) | E7 | Dashboard shows real analytics from real usage |
| **M8** (~wk 28) | E8 | Deployed, demo-ready, README + case study written |

**M5 (full RAG loop working) is the single most important milestone** — it's the point where the project becomes genuinely demo-able and portfolio-worthy even if E6–E8 aren't done yet. If time gets tight, this is the fallback "good enough to show" point.

---

## 4. Epic 0 — Infrastructure & Project Setup

**Feature: Repo & local dev environment**
- [ ] Task: Create monorepo structure per Doc 09 §3 (1 hr)
- [ ] Task: Write `docker-compose.yml` with Postgres+pgvector, Redis, Ollama (1.5 hr)
- [ ] Task: Verify `docker-compose up` brings everything up cleanly (0.5 hr)
- [ ] Task: Set up Flyway in one service as a template for others (1.5 hr)
- [ ] Task: Create `.env.example` and gitignore rules per Doc 08 §2 (0.5 hr)

**Feature: CI pipeline skeleton**
- [ ] Task: GitHub Actions workflow — lint + test on push (1.5 hr)
- [ ] Task: Path-based triggers per service (Doc 09 §4) (1 hr)
- [ ] Task: Docker image build step (1.5 hr)

**Feature: Gateway skeleton**
- [ ] Task: Spring Boot gateway service, basic routing to a stub service (2 hr)
- [ ] Task: JWT validation middleware skeleton (no real auth yet, just structure) (1.5 hr)

**Feature: Shared common module** *(added mid-Epic 1, once per-endpoint envelope classes became repetitive)*
- [x] Task: Create `services/common` Maven module — `RequestEnvelope<T>`, `ApiResponse<T>`, `ApiError`, `PaginationMeta` (Doc 05 §1 conventions) (1.5 hr)
- [x] Task: Install locally, wire into `identity` service, delete per-endpoint envelope classes (1 hr)
- [ ] Task: Add `common` build step to CI pipeline once CI is implemented (Doc 09 §4 — open task, not blocking now) (0.5–1.5 hr depending on approach chosen)

---

## 5. Epic 1 — Authentication & Identity

**Feature: Registration & login**
- [ ] Task: `app_user` table + Flyway migration (0.5 hr)
- [ ] Task: `POST /auth/register` — validation, password hashing (argon2id) (1.5 hr)
- [ ] Task: `POST /auth/login` — credential check, JWT issuing (1.5 hr)
- [ ] Task: Refresh token table + `POST /auth/refresh` with rotation (Doc 06 §2) (2 hr)
- [ ] Task: `POST /auth/logout` — revoke refresh token (0.5 hr)

**Feature: Email verification & password reset**
- [ ] Task: Notification service skeleton + email provider integration (2 hr)
- [ ] Task: Email verification token flow (1.5 hr)
- [ ] Task: Forgot/reset password flow, no user enumeration (Doc 08 §6) (1.5 hr)

**Feature: Google OAuth**
- [ ] Task: Google OAuth flow per Doc 06 §5, account linking logic (2.5 hr)

**Feature: Dashboard auth UI**
- [ ] Task: Next.js login/register pages (2 hr)
- [ ] Task: Token storage — httpOnly cookie wiring per Doc 08 §3 (1.5 hr)
- [ ] Task: Auth context/hook for the dashboard app (1 hr)

---

## 6. Epic 2 — Organization & Team Management

**Feature: Org creation**
- [ ] Task: `organization` + `membership` tables, RLS policies (Doc 04 §4) (2 hr)
- [ ] Task: `POST /orgs`, `GET /orgs/current` (1.5 hr)
- [ ] Task: `SET LOCAL app.current_org_id` wiring per Doc 06 §3 — this is the one to get right (2 hr)

**Feature: Team invites**
- [ ] Task: Invite flow incl. edge cases from Doc 06 §4 (2.5 hr)
- [ ] Task: Role management endpoints (change role, remove member) (1.5 hr)
- [ ] Task: Dashboard UI — team settings page (2 hr)

**Feature: API keys**
- [ ] Task: API key generation/hashing/revocation endpoints (1.5 hr)

---

## 7. Epic 3 — Knowledge Base

**Feature: Document upload & processing**
- [ ] Task: `knowledge_doc`, `knowledge_chunk` tables + pgvector extension setup (1.5 hr)
- [ ] Task: File upload endpoint + storage (multipart handling, validation per Doc 08 §4) (2 hr)
- [ ] Task: `async_job` table + polling worker skeleton (Doc 04 §3) (2.5 hr)
- [ ] Task: Text extraction (PDF/DOCX/TXT parsers) (2.5 hr)
- [ ] Task: Chunking logic per Doc 07 §2 (2 hr)
- [ ] Task: Embedding generation call (Ollama locally) (1.5 hr)
- [ ] Task: Store chunks + embeddings, update document status (1.5 hr)

**Feature: Knowledge management UI**
- [ ] Task: Upload UI + processing status display (2 hr)
- [ ] Task: Document list/delete UI (1.5 hr)
- [ ] Task: Manual FAQ entry CRUD (1.5 hr)

**Feature: Search**
- [ ] Task: `/knowledge/search` internal endpoint, similarity query per Doc 07 §4 (2 hr)
- [ ] Task: Manual testing/tuning of similarity threshold (1 hr)

---

## 8. Epic 4 — Chat Widget (frontend)

**Feature: Embeddable widget core**
- [ ] Task: Widget script scaffold, floating bubble UI (2.5 hr)
- [ ] Task: `POST /widget/conversations` integration, session token storage (1.5 hr)
- [ ] Task: WebSocket connection handling (2 hr)
- [ ] Task: Message send/receive UI, mobile responsive (2.5 hr)
- [ ] Task: Typing indicator, branding config (color/greeting) (1.5 hr)

**Feature: Widget embed & distribution**
- [ ] Task: Build/bundle widget as a standalone script, host on CDN (2 hr)
- [ ] Task: Test embed page (plain HTML) to verify it works on a third-party-style page (1 hr)
- [ ] Task: Generate embed snippet in dashboard for each org (1 hr)

---

## 9. Epic 5 — AI / RAG Chat

**Feature: Provider abstraction**
- [ ] Task: `AIProvider` interface + `OllamaProvider` implementation (2.5 hr)
- [ ] Task: `GeminiProvider` implementation (2 hr)
- [ ] Task: Config-based provider switching (Doc 07 §5) (1 hr)

**Feature: RAG response pipeline**
- [ ] Task: `chat-service` publishes `ai_response` async_job on visitor message (1.5 hr)
- [ ] Task: `ai-service` worker consumes job, calls knowledge search (2 hr)
- [ ] Task: Prompt construction per Doc 07 §6 (1.5 hr)
- [ ] Task: LLM call + response parsing (2 hr)
- [ ] Task: Fallback response when no chunks clear threshold (Doc 07 §4) (1.5 hr)
- [ ] Task: Write AI message back to conversation, push via WebSocket (2 hr)

**Feature: Tuning & quality**
- [ ] Task: End-to-end manual testing with real uploaded docs (2 hr)
- [ ] Task: Tune chunk size / threshold / prompt based on real results (2 hr)
- [ ] Task: Rate limiting on widget endpoints per Doc 05 §8 (1.5 hr)

---

## 10. Epic 6 — Human Takeover / Live Chat

**Feature: Agent-facing live queue**
- [ ] Task: `GET /conversations` with status/mode filters (1.5 hr)
- [ ] Task: Dashboard live queue UI (2 hr)
- [ ] Task: WebSocket updates for new/incoming conversations (2 hr)

**Feature: Takeover mechanics**
- [ ] Task: `POST /conversations/{id}/claim` — pause AI, assign agent (1.5 hr)
- [ ] Task: System message on takeover ("Agent X joined") (1 hr)
- [ ] Task: Agent message send endpoint + UI (2 hr)
- [ ] Task: Release/resolve conversation endpoints (1.5 hr)
- [ ] Task: Visitor-side "request human" trigger in widget (1.5 hr)

---

## 11. Epic 7 — Dashboard & Analytics

- [ ] Task: `analytics/summary` endpoint — conversations today, AI vs human resolved (2 hr)
- [ ] Task: `analytics/top-questions` — derive from message logs (2 hr)
- [ ] Task: `analytics/response-time` calculation (1.5 hr)
- [ ] Task: Dashboard home page pulling all three together (2.5 hr)
- [ ] Task: Basic charts (conversations over time) (2 hr)

---

## 12. Epic 8 — Deployment, Polish, Demo-Readiness

- [ ] Task: Deploy dashboard to Vercel (1 hr)
- [ ] Task: Deploy backend services to Railway/Render (2.5 hr)
- [ ] Task: Neon + Upstash production wiring, run migrations (1.5 hr)
- [ ] Task: Gemini API key setup for staging (0.5 hr)
- [ ] Task: End-to-end smoke test on deployed environment (2 hr)
- [ ] Task: Seed demo data (fake org, sample knowledge base) for instant demoability (1.5 hr)
- [ ] Task: README with architecture overview, screenshots, setup instructions (2 hr)
- [ ] Task: Write a short case-study writeup (problem → architecture → trade-offs) for portfolio site (1.5 hr)

---

## 13. Working Agreement (how to actually use this doc)

- Work top-to-bottom within an epic; epics are ordered by dependency, not by interest — resist the urge to jump to E5 (AI, the "fun" part) before E1–E3 are solid, or you'll be debugging auth issues while trying to debug AI issues.
- Check off tasks as you go — this doc is the single source of truth for "what's next," so you never have to re-decide during a limited evening session.
- If a task takes meaningfully longer than estimated, that's fine — the estimates are for planning, not for judging yourself. Adjust the milestone dates, not your standards.
- Re-visit this doc weekly (5 min) to update `Last updated` and re-confirm what's next.

---

## 14. Next Steps

→ **Doc 11: Sprint/Milestone Calendar** — optional, only useful if you want actual calendar dates attached to the milestones above rather than relative week numbers. Otherwise, planning is complete and E0 Task 1 is the next real action.
