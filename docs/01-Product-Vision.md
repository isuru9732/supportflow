# 01 — Product Vision & Scope

**Status:** Draft v1.0
**Owner:** Isuru
**Last updated:** 2026-07-01

---

## 1. Working Name

**SupportFlow AI** (placeholder — revisit once domain availability is checked)

## 2. Vision Statement

A self-serve AI customer support platform that lets any small business embed a chat widget on their website, answer customer questions automatically from their own knowledge base, and hand off to a human when the AI can't help — without the cost or complexity of enterprise tools like Intercom or Zendesk.

## 3. Problem Statement

Small businesses field the same repetitive questions (hours, pricing, booking, cancellation policy) across chat, phone, and email, with no dedicated support staff. Existing tools are priced and built for larger teams. There is no simple, affordable, AI-first option that a non-technical business owner can set up in an afternoon.

## 4. Dual Goals

This project is being built with two goals simultaneously — both must hold at every design decision:

| Goal | What it requires |
|---|---|
| **Portfolio-grade** | Clean multi-service architecture, real AI/RAG implementation, proper auth & multi-tenancy, CI/CD, tests, documentation a senior engineer would recognize as production-minded |
| **Commercially viable** | Actually onboardable by a real non-technical business owner; usage-based cost control; extensible without rewrites |

## 5. Target Market — Phase 1

**Decision:** Vertical-agnostic ("suite for any business") rather than a single niche.

**Rationale:** Because the AI answers strictly from each tenant's own uploaded knowledge base, the core product is naturally vertical-agnostic — a car workshop's FAQs and a hotel's FAQs are just different documents in different tenants. No architecture changes needed per vertical.

**Trade-off accepted:** Broad positioning is a harder go-to-market story than a niche ("the AI support tool for car workshops"). This is a marketing decision only — it can be narrowed later for outreach/landing-page copy without touching the product. Not a blocker for Phase 1 build.

## 6. MVP Scope (Phase 1) — In / Out

### In scope
- Org (tenant) registration & workspace creation
- User auth (email/password + Google login), roles: Owner, Admin, Agent
- Embeddable chat widget (script tag, floating bubble, mobile responsive)
- Knowledge base: upload PDF/DOCX/TXT, auto-chunk + embed
- AI chat: RAG-based Q&A against tenant's knowledge base only
- Human takeover: agent can join and take over a live conversation
- Basic dashboard: conversations today, resolved by AI vs human, most-asked questions
- Multi-tenant data isolation (shared schema + `tenant_id` + Postgres RLS)
- AI provider abstraction (swap Gemini ↔ Ollama ↔ OpenAI via config)

### Explicitly OUT of scope for Phase 1
- Ticketing system
- Email/WhatsApp/Slack channel integrations
- Billing/subscription enforcement (design for it, don't build it yet)
- CSAT surveys, sentiment analysis, auto-ticket classification
- Departments, tags, macros
- Voice/speech-to-text

These are documented so scope creep has a clear boundary — anything not listed above does not get built until Phase 1 ships.

## 7. Primary Personas

1. **Org Owner/Admin** — signs up, sets up workspace, uploads knowledge base, invites agents, views dashboard. Non-technical.
2. **Support Agent** — logs in, monitors live conversations, takes over from AI when needed.
3. **End Customer** — visitor on the business's website, interacts with the widget, no login required.
4. **Platform Admin (you)** — internal role for managing tenants, monitoring system health (Phase 1: minimal, just enough to operate).

## 8. Success Criteria for Phase 1

- A new org can sign up, create a workspace, upload a knowledge base, embed the widget, and get an AI-answered conversation working — all without your manual intervention.
- Demo-able end-to-end in a live interview or to a prospective business owner.
- Runs at $0 cost using free tiers (Vercel, Railway/Render, Neon, Upstash, Gemini free tier or local Ollama).

## 9. Key Architecture Decisions (locked here, detailed in later docs)

| Decision | Choice | Detailed in |
|---|---|---|
| Multi-tenancy | Shared schema + `tenant_id` + Postgres Row-Level Security | Doc 07 |
| AI provider | Abstracted interface; Gemini (prod default) / Ollama (dev + fallback) | Doc 08 |
| Service structure | Modular monolith-leaning services (gateway, identity, organization, chat, knowledge, ai, notification) — not micro-fragmented | Doc 05/06 |
| Frontend | Next.js 14, deployed on Vercel | Doc 05 |
| Backend | Spring Boot | Doc 05 |
| Database | PostgreSQL + pgvector | Doc 05 |

## 10. Assumptions & Constraints

- Solo developer, ~5–8 hrs/week, evenings only.
- No budget for paid infra during build/demo phase — every choice must have a free tier that's realistic to run continuously.
- No committed pilot customer yet — Phase 1 is built to be demo-ready, with real-customer onboarding as a stretch goal once functional.

## 11. Next Steps

→ Doc 02: Software Requirements Specification (functional + non-functional requirements, detailed user stories)
