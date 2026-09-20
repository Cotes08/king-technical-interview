# AI Usage Disclosure

This document describes how AI assistance was used during the development of
this assignment. It is provided in response to the assignment's requirement:

> *"If you have used AI tooling for the development, please include the
> artifacts or documents generated."*

The honest summary: AI was used as a **senior reviewer and pair-programming
partner** with full context of the assignment. I wrote the production code
myself and used AI mainly for design discussions, code review, and test
implementation. The documentation was drafted with AI assistance because
the collaborator had the full context of every decision made along the way.

---

## Tools used

- **Claude (Anthropic)** — used throughout the assignment for design
  discussions, code review, test scaffolding, and documentation drafting.

---

## How the collaboration worked

The workflow was **iterative** and centered on my own writing:

1. I described a problem or shared a class I had written.
2. Claude asked clarifying questions and proposed options with trade-offs.
3. I chose the direction.
4. I wrote the code myself, based on the discussion.
5. I shared it back for review.
6. Claude pointed out issues; I applied the fixes.

This felt like pairing with a colleague who had the whole conversation in
mind. Several decisions were revisited multiple times before landing.

---

## 1. Design decisions

Design was discussed extensively before any code was written. Concrete
examples:

- **Layered vs. Hexagonal architecture** → chose layered because the scope
  (two endpoints, one use case, in-memory storage) did not justify the
  indirection of ports and adapters.
- **DTO vs. domain separation** → kept them separate. The deciding argument
  was that `Integer minimumLevel` + `@NotNull` catches missing JSON fields,
  while a primitive `int` would default to `0` and silently make all players
  eligible.
- **Storage model** ("recipe, not cake") → store rules + player IDs and
  recalculate on read, so that retrieval can reflect current rules.
- **Idempotency strategy** → return 200 for identical repeats, 409 for
  conflicting bodies.
- **Unknown player handling** → reject the whole request with 400 and list
  **all** unknown IDs, rather than evaluating what is valid.

Each of these was debated back and forth. The final choice was always mine,
but the reasoning was sharpened through the dialogue.

---

## 2. Production code

All production code was written by me. Concretely:

- **Domain** (`PlayerProfile`, `CampaignRules`, `StoredEvaluation`).
- **Repositories** (`PlayerRepository`, `CampaignEvaluationRepository`).
- **Service** (`CampaignEvaluationService`) and the service exceptions.
- **API layer** (controller, DTOs, `ErrorResponse`,
  `GlobalExceptionHandler`).

The role of AI here was **review and feedback**, not generation. Concrete
changes I applied after review feedback:

- `HashMap` + `containsKey` + `put` → `ConcurrentHashMap.putIfAbsent` to
  eliminate a race condition in `CampaignEvaluationRepository`.
- `List<String>` instead of `String[]` in `CampaignRules` (records with
  array fields have broken `equals`/`hashCode`, which would break conflict
  detection).
- Typo fix: `CampaingRules` → `CampaignRules`.
- Moved fixture loading from `@PostConstruct` to the constructor in
  `PlayerRepository` so unit tests can instantiate it without Spring.
- Ensured `toResponse()` reads from the **stored** `StoredEvaluation`
  rather than the incoming request, so POST and GET responses stay
  consistent.
- Replaced duplicated eligibility logic with a call to
  `CampaignRules.isPlayerEligible(...)`.

Each change was understood and applied by me after discussion.

---

## 3. Tests — significant AI assistance

The test suite is where AI assistance was most substantial. I wrote the
tests but Claude was directly involved in shaping several of them:

- **`CampaignRulesTest`**: provided the initial structure (`@Nested` +
  `@DisplayName` pattern). I adapted the cases and assertions.
- **`CampaignEvaluationRepositoryTest`**: provided a full draft of the
  concurrency test using `CountDownLatch`. I reviewed it, ran it, and
  modified it:
  - The first draft used `executor.shutdown()` without awaiting thread
    completion, which made the assertion race-prone. I added a second
    `CountDownLatch` (`endGate`) so the main thread waits for all workers.
  - Replaced `AtomicInteger` with a `CopyOnWriteArrayList` to also assert
    that all losers received the same stored value.
  - Restored the interrupt flag in the `catch` block
    (`Thread.currentThread().interrupt()`).
  - I validated the test by temporarily replacing `putIfAbsent` with `put`
    and confirming the test fails — then reverting.
- **`CampaignEvaluationControllerTest`**: I chose the
  `standaloneSetup` approach myself; Claude provided early feedback on the
  trade-offs vs. `@WebMvcTest` / `@SpringBootTest`, and we discussed the
  pros and cons. The final structure is mine.
- **`CampaignEvaluationIntegrationTest`** and
  **`CampaignEvaluationServiceTest`**: I combined drafts from Claude with
  my own modifications and additional cases.

The **choice of which cases to cover, how to group them, and what to
assert** reflects my own reasoning. Claude accelerated the boilerplate.

---

## 4. Documentation — drafted with AI, reviewed by me

The `README.md` was drafted with Claude's help. Since the entire design
discussion happened in the same conversation, Claude had the full context
and could produce documentation that matched the code.

What I did with the draft:

- Verified that every claim in the README matches the actual code.
- Adjusted phrasing where needed.
- Corrected details (jar name, package paths, test pyramid description).
- Owned the final document.

The `What I Would Do Next` section and the `Ambiguities Identified` table
reflect decisions I had reasoned through; the text was drafted by AI to
express them clearly.

I consider the documentation a genuine deliverable of the collaboration.
Writing good technical docs is part of the job, and using a contextual
collaborator to accelerate the drafting is no different from using a linter
or a formatter.

---

## 5. What was NOT delegated to AI

- No production class was generated end-to-end and pasted.
- Every method in `CampaignEvaluationService` was written by me after
  discussion and review.
- The final architectural decisions were mine.
- I can explain every line of code and every test in this repository.

---

## 6. Findings from my own testing

- Bean Validation reports container element constraint violations with a
  `<list element>` suffix in the field path (e.g.
  `playerIds[0].<list element>`). I discovered this while debugging an
  integration test and documented it in the README.

---

## Verification

- I can explain every method and test in this repository line by line.
- The concurrency test was validated by intentionally breaking the
  repository implementation and confirming the test fails.
- All design decisions in the README have a documented rationale I can
  defend in an interview setting.

---

If any of this raises questions in the interview, I am happy to walk
through any specific file or decision in detail.