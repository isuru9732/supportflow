# SupportFlow AI — Agent Instructions

AI customer support SaaS. Spring Boot microservices (Java 17, Maven) + Next.js dashboard.
Full design docs live in `docs/` (Docs 01–10). Current task list: `docs/10-Agile-Backlog.md`.

## Reading the docs — on demand, not all at once

Don't load every doc into context every session — that wastes tokens on a free-tier
model for no benefit on simple tasks. Instead, **read the specific doc that matches
the task before writing code**:

| Working on... | Read first |
|---|---|
| Any new entity/table/migration | `docs/04-Database-Design.md` |
| Any new endpoint | `docs/05-API-Design.md` |
| Auth, JWT, org context/RLS | `docs/06-Auth-Multi-Tenancy.md` |
| Anything touching secrets, tokens, validation | `docs/08-Security-Architecture.md` |
| New service, Docker, CI, Flyway config | `docs/09-Deployment-CICD.md` |
| "What's next" / picking a task | `docs/10-Agile-Backlog.md` |
| Unsure how a service fits together | `docs/03-System-Architecture.md` |

If a doc and the actual code disagree, **say so and ask** — don't silently follow
one or the other.

## Hard rules — do not deviate without asking

1. **Never invent a new envelope/response shape.** Every request body is
   `RequestEnvelope<T>` and every response is `ApiResponse<T>` — both from the
   shared `services/common` module (`com.supportflow.common.dto` /
   `com.supportflow.common.response`). Do not create a per-endpoint `XxxEnvelope`
   class — this was already tried and refactored away. See Doc 05 §1.
2. **Follow the layered structure exactly:** `entity/`, `repository/`,
   `service/` (interface) + `service/impl/` (implementation), `controller/`,
   `dto/`, `exception/`. No business logic in controllers. No direct repository
   calls from controllers.
3. **Never touch identity's RSA private key or copy it into another service.**
   Only `public_key.pem` gets copied to services that verify tokens.
4. **Never commit `.env`, `*.pem` files, or anything under `resources/keys/`.**
   Already gitignored — do not remove those gitignore entries.
5. **Do not modify a working, tested endpoint's behavior without explicit
   instruction.** If asked to add something nearby, add it — don't "clean up"
   or refactor adjacent working code as a side effect.
6. **Every new Spring Boot service that shares the Postgres database MUST set
   three properties** in `application.properties` before its first migration
   will work (see Doc 09 §5 for why — this cost real debugging time once):
   ```properties
   spring.flyway.table=flyway_schema_history_<service_name>
   spring.flyway.baseline-on-migrate=true
   spring.flyway.baseline-version=0
   ```
7. **Every new service depending on `common` MUST add explicit component
   scanning** in its main `@SpringBootApplication` class:
   ```java
   @ComponentScan(basePackages = {"com.supportflow.<service>", "com.supportflow.common"})
   ```
   Without this, `common`'s `@Component` beans (e.g. `JwtVerifier`) silently
   fail to load with a confusing "bean not found" error.
8. **Org-scoping pattern (until the Gateway is built out — see Doc 03 §7):**
   org-scoped endpoints take an `X-Org-Id` header, checked against the
   `membership` table, then `SET LOCAL app.current_org_id` for that
   transaction before any query runs. Don't skip the membership check.

## Commands

```bash
# Start infra
docker compose up -d postgres redis ollama

# Build/install the shared library BEFORE building any service that uses it
cd services/common && mvn clean install

# Build + run a service
cd services/<name> && mvn clean install && mvn spring-boot:run

# Check DB state
docker exec -it supportflow-postgres psql -U supportflow -d supportflow -c '\dt'
```

## Workflow expectations

- **Small, reviewable changes.** One endpoint or one migration at a time, not
  a sweeping multi-file change, unless explicitly asked for a refactor.
- **Explain non-obvious decisions in a code comment**, the way the existing
  code does (e.g. why `SET LOCAL` not `SET`, why password check happens
  before verification check). Don't just write code silently.
- **If something in `docs/` conflicts with what you're about to do, stop and
  say so** rather than picking one silently.
- I review every diff before accepting. Don't assume unstaged/uncommitted
  work is safe to build on top of without checking `git status` first.

## Stack reference

Java 17, Spring Boot 3.5.x, PostgreSQL 16 + pgvector, Redis, Flyway, Argon2id
(password hashing), RS256 JWT, Next.js 14 (dashboard, not yet built).
