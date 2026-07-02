# 07 — AI / RAG Architecture

**Status:** Draft v1.0
**References:** 02-SRS.md, 03-System-Architecture.md, 04-Database-Design.md, 06-Auth-Multi-Tenancy.md
**Last updated:** 2026-07-01

---

## 1. Pipeline Overview

```
UPLOAD                                          QUERY TIME
───────────────────────────                     ───────────────────────────
Document uploaded
     │
     ▼
async_job (document_ingestion)
     │
     ▼
Extract text (PDF/DOCX/TXT parser)
     │
     ▼
Chunk text (~500 tokens, 50 token overlap)
     │
     ▼
Generate embedding per chunk
     │
     ▼
Store in knowledge_chunk (pgvector)
                                                  Visitor sends message
                                                       │
                                                       ▼
                                                  Embed the visitor's question
                                                       │
                                                       ▼
                                                  Similarity search (org-scoped)
                                                       │
                                                       ▼
                                                  Top-K chunks (K=4, threshold-filtered)
                                                       │
                                                       ▼
                                                  Construct prompt (context + question)
                                                       │
                                                       ▼
                                                  Call LLM provider
                                                       │
                                                       ▼
                                                  Return answer + source chunk IDs
```

---

## 2. Chunking Strategy

- **Chunk size:** ~500 tokens, with ~50 token overlap between consecutive chunks (prevents losing context at chunk boundaries — a sentence split across two chunks still has surrounding context in each).
- **Chunking approach:** split on paragraph boundaries first, then merge/split to hit target size — avoids cutting mid-sentence where possible.
- **Per-document metadata retained:** `document_id`, `chunk_index` (for reconstructing order if needed), `org_id` (tenant scoping, enforced by RLS from Doc 04/06).

## 3. Embedding Model

**Decision:** use the embedding model that ships with whichever provider is active, rather than a separate fixed embedding service — keeps the free-tier story simple (no separate embedding API to manage/pay for).

| Provider | Embedding model | Dimensions |
|---|---|---|
| Gemini | `text-embedding-004` | 768 |
| Ollama (local) | `nomic-embed-text` | 768 |

**Important constraint:** the `VECTOR(768)` column defined in Doc 04 assumes both options produce 768-dim vectors — chosen deliberately so switching providers doesn't require a schema migration. If a future provider uses a different dimension, that's a breaking schema change — documented here so it's not a surprise later.

## 4. Similarity Search & Fallback Threshold

```sql
SELECT id, content, document_id,
       1 - (embedding <=> :query_embedding) AS similarity
FROM knowledge_chunk
WHERE org_id = :org_id
ORDER BY embedding <=> :query_embedding
LIMIT 4;
```

- **Threshold:** chunks with similarity below **0.65** (tunable, empirical) are discarded even if they're in the top 4 — better to return fewer chunks or none than to feed irrelevant context to the LLM.
- **Fallback (FR-5.3):** if zero chunks clear the threshold, skip the LLM call entirely and return a canned response offering human handoff:
  > "I don't have an answer for that in our knowledge base yet — would you like me to connect you with a member of our team?"
  This also saves an LLM call (cost control) when it clearly won't help.

## 5. Provider Abstraction Interface

```java
public interface AIProvider {
    EmbeddingResult embed(String text);
    ChatResponse generateAnswer(String systemPrompt, String context, String userQuestion);
}

public class GeminiProvider implements AIProvider { /* ... */ }
public class OllamaProvider implements AIProvider { /* ... */ }
public class OpenAIProvider implements AIProvider { /* ... */ }
```

- Provider selected via config (`application.yml` / env var: `AI_PROVIDER=gemini|ollama|openai`), resolved at startup via Spring's `@ConditionalOnProperty` or a simple factory — no code changes to switch.
- **Per-org override** deferred: FR-5.4 only requires global swap for Phase 1, not per-tenant provider choice (that would need a `preferred_provider` column on `organization` — not currently in Doc 04's schema, noting as a Phase 2 candidate rather than adding scope now).

## 6. Prompt Construction

```
SYSTEM PROMPT:
You are a helpful support assistant for {orgName}. Answer only using the
context provided below. If the context doesn't contain the answer, say
you don't know — do not guess or make up information.

CONTEXT:
{retrieved chunk 1}
{retrieved chunk 2}
{retrieved chunk 3}
{retrieved chunk 4}

CUSTOMER QUESTION:
{visitorMessage}
```

**Why an explicit "don't guess" instruction, on top of the similarity threshold:** two independent safety nets — the threshold prevents obviously irrelevant context from reaching the model, and the prompt instruction reduces hallucination even when retrieved context is marginally relevant but doesn't fully answer the question. Belt and suspenders, deliberately.

## 7. Cost & Rate Control

- Widget-facing rate limiting (Doc 05 §8) is the primary lever — caps LLM calls per org per time window.
- Fallback-before-LLM-call (§4) reduces wasted calls on unanswerable questions.
- **Conversation-level context window:** only the current question + retrieved chunks sent per call — NOT full conversation history on every turn (that scales cost linearly with conversation length for no real benefit at MVP stage). If follow-up-question coherence becomes a real problem, revisit with a summarization step — not built into Phase 1.

## 8. Confidence Score (Message.source_chunks field usage)

The `confidence` field returned by the AI Service (Doc 05 §6 example) is the top chunk's similarity score, stored alongside the message. This isn't shown to the visitor — it's for the dashboard "most-asked questions" and quality-review tooling later (FR-7.2), so low-confidence answers can be surfaced to the Org Admin as knowledge-base gaps worth filling.

---

## 9. Next Steps

→ **Doc 08: Security Architecture** (threat model, secrets management, input sanitization, rate limiting detail)
