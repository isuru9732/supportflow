# 08 — Security Architecture

**Status:** Draft v1.0
**References:** 02-SRS.md (NFR-1, NFR-2), 04-Database-Design.md, 05-API-Design.md, 06-Auth-Multi-Tenancy.md, 07-AI-RAG-Architecture.md
**Last updated:** 2026-07-01

---

## 1. Threat Model (Phase 1 scope)

Lightweight STRIDE-style pass — not exhaustive, but covers what actually matters for an MVP with real user data.

| Threat | Vector | Mitigation | Doc reference |
|---|---|---|---|
| Cross-tenant data leakage | Bug in application-layer filtering | Postgres RLS as defense-in-depth, not the only line of defense | Doc 04 §4, Doc 06 §3 |
| Credential stuffing / brute force | Repeated login attempts | Rate limiting on `/auth/login`, generic error messages (no "email not found" vs "wrong password" distinction) | Doc 06 §6 |
| Session/token theft | XSS, token stored insecurely | Refresh token rotation + revocation detection; access token short-lived; no tokens in localStorage for dashboard (httpOnly cookie preferred — see §3) | Doc 06 §2 |
| API key leakage (widget) | Key exposed in public page source (expected — it's client-side by design) | Widget API key is scoped to "send messages + read own conversation" only, cannot access dashboard/admin endpoints even if leaked | Doc 05 |
| Prompt injection via visitor message | Visitor crafts a message trying to override the system prompt (e.g., "ignore previous instructions") | System prompt explicitly scopes the model's role; retrieved context is clearly delimited from user input; treat as a known residual risk, not fully solvable — documented rather than ignored | Doc 07 §6 |
| Malicious file upload | Uploading a disguised executable as "knowledge document" | File type validated by content inspection (not just extension), size-limited, processed in an isolated worker context, never executed | FR-3.6 |
| SQL injection | Raw string concatenation in queries | All queries parameterized (JPA/prepared statements) — zero raw concatenation, enforced by code review discipline since this is a solo project | — |
| Denial of wallet (LLM cost abuse) | Attacker spams the widget endpoint to burn AI provider quota | Rate limiting per API key (Doc 05 §8) + fallback-before-LLM-call (Doc 07 §4) | Doc 05, Doc 07 |
| Insecure direct object reference | Guessing conversation/document IDs across tenants | UUIDs (not sequential IDs) + RLS ensures even a guessed UUID returns nothing outside the requester's org | Doc 04 |

## 2. Secrets Management

| Secret | Storage (dev) | Storage (prod) |
|---|---|---|
| DB credentials | `.env` (gitignored) | Platform env vars (Railway/Render secrets) |
| LLM API keys (Gemini/OpenAI) | `.env` | Platform env vars |
| JWT signing key (RS256 keypair) | `services/identity/src/main/resources/keys/` (gitignored — see below) | Platform env vars holding key contents, or a secrets manager; **never baked into a Docker image** |
| RSA public key (verification only) | Copied into each verifying service's `resources/keys/public_key.pem` | Same — public key only, safe to distribute more widely than the private key, but still not committed to git as a matter of consistent practice |
| Google OAuth client ID | `.env` (`GOOGLE_OAUTH_CLIENT_ID`) | Platform env vars — **no client secret needed** at all for our ID-token verification flow (Doc 06 §5) |
| Widget API keys (per-tenant) | N/A — generated at runtime | Hashed in DB (`api_key.key_hash`), plaintext shown to user once at creation only |

**Rule:** nothing secret ever committed to git. `.env.example` with placeholder keys committed instead, real `.env` gitignored from day one of the repo.

**RSA keypair specifics (added during Epic 1/2 implementation):** the private key lives only in `identity` service (the signer); every other service needing to verify tokens gets **only the public key**, copied manually into its own resources during local dev. `services/*/src/main/resources/keys/` is gitignored entirely — confirmed in the root `.gitignore`. **Production task (not yet done):** figure out the actual secret-distribution mechanism for the private key and every service's copy of the public key before deploying — env-var-embedded PEM content is the simplest option on Railway/Render, but decide and document this explicitly before the first real deploy rather than improvising under time pressure.

## 3. Token Storage — Dashboard vs Widget

- **Dashboard (Next.js):** access token kept in memory (React state/context), refresh token in an **httpOnly, secure, sameSite=strict cookie** — not accessible to JavaScript, meaningfully reduces XSS token-theft risk compared to localStorage.
- **Widget (embedded on third-party sites):** `visitorSessionToken` in localStorage is acceptable here — it only grants access to that visitor's own anonymous conversation, not account access, so the blast radius of theft is minimal.

This is a deliberate distinction, not an inconsistency — worth being able to explain why the two contexts are handled differently.

## 4. Input Validation & Sanitization

- All API request bodies validated against schema (Bean Validation / `@Valid` in Spring Boot) before touching business logic.
- Visitor chat messages: length-capped, HTML-stripped before storage (messages are rendered as plain text in the dashboard/widget, never as raw HTML, which closes off stored-XSS via chat).
- File uploads: MIME type verified by content sniffing, not just filename extension; max size enforced at the gateway level before the file even reaches the service.

## 5. Transport & Infrastructure

- HTTPS enforced everywhere (Vercel/Railway/Render provide this by default on their platforms — verify, don't assume).
- CORS: widget endpoints allow cross-origin by design (embedded on client websites) but scoped to POST/GET only, no credentials mode; dashboard endpoints restrict CORS to the known dashboard origin only.
- Database connections use SSL (Neon enforces this by default).

## 6. Audit Logging

Per the `audit_log` table (Doc 04): logged actions include login, role changes, member removal, document deletion, API key creation/revocation. Not logged: routine chat messages (would bloat the table and isn't a security-relevant event) — this is a deliberate scoping decision, not an oversight.

## 7. What's Explicitly Deferred (documented, not silently skipped)

- **MFA** — Phase 2 (per Doc 02 FR-1.7).
- **Penetration testing / formal security audit** — not realistic for a solo portfolio project pre-launch; would be a prerequisite before onboarding a real paying customer with sensitive data.
- **GDPR/data residency tooling** — out of scope until there's an actual EU customer; flagged so it's a known gap, not forgotten.
- **Full prompt-injection hardening** — treated as residual risk per §1; revisit if the AI ever gets tool-use/function-calling capabilities (much higher stakes than pure text generation).

---

## 8. Next Steps

→ **Doc 09: Deployment & CI/CD Architecture** (Docker Compose dev setup, GitHub Actions pipeline, environment promotion strategy)
