# Campaign Evaluations Service

A small Spring Boot service that evaluates a set of players against a
campaign's eligibility rules and stores the evaluation result for later
retrieval.

A campaign defines a **minimum level** and a list of **countries**. A player
is eligible when their country is in the campaign's list **AND** their level
is greater than or equal to the campaign's minimum level.

---

## Tech Stack

- **Java 21** (Temurin)
- **Spring Boot 4.1.1**
- **Maven 3.9.16**
- **JUnit 5 + AssertJ + MockMvc + Mockito** for testing
- In-memory storage only (no database — persistence across restarts is not
  required by the assignment)

---

## How to Run

```bash
# From the project root
./mvnw spring-boot:run
```

The service listens on `http://localhost:8080`.

### Run the tests

```bash
./mvnw clean test
```

### Package and run the jar

```bash
./mvnw clean package
java -jar target/king-exercise-0.0.1-SNAPSHOT.jar
```

---

## API Reference

### `POST /campaign-evaluations`

Evaluates a campaign for a list of players and stores the evaluation under
the given `evaluationId`.

**Request body**

```json
{
  "evaluationId": "eval-1",
  "campaignRules": {
    "campaignId": "summer-boost",
    "minimumLevel": 10,
    "countries": ["ES", "SE"]
  },
  "playerIds": ["p-42", "p-77", "p-88", "p-99", "p-101"]
}
```

**Response 200**

```json
{
  "evaluationId": "eval-1",
  "campaignId": "summer-boost",
  "results": [
    { "playerId": "p-42",  "eligible": true  },
    { "playerId": "p-77",  "eligible": false },
    { "playerId": "p-88",  "eligible": true  },
    { "playerId": "p-99",  "eligible": true  },
    { "playerId": "p-101", "eligible": false }
  ]
}
```

**Error responses**

| Status | When |
|---|---|
| `400 Bad Request` | Missing/blank fields, empty `playerIds` or `countries`, `minimumLevel < 1`, or one or more unknown player IDs |
| `409 Conflict` | Same `evaluationId` already exists with a different body |

### `GET /campaign-evaluations/{evaluationId}`

Returns the stored evaluation. Eligibility is recalculated from the stored
rules every time it is retrieved.

**Response 200**: same shape as the POST response.

**Error responses**

| Status | When |
|---|---|
| `404 Not Found` | The `evaluationId` does not exist |

### Example with curl

```bash
# Create
curl -X POST http://localhost:8080/campaign-evaluations \
  -H "Content-Type: application/json" \
  -d '{
    "evaluationId": "eval-1",
    "campaignRules": {
      "campaignId": "summer-boost",
      "minimumLevel": 10,
      "countries": ["ES", "SE"]
    },
    "playerIds": ["p-42", "p-77", "p-88", "p-99", "p-101"]
  }'

# Retrieve
curl http://localhost:8080/campaign-evaluations/eval-1
```

### Player fixture

The service ships with six in-memory player profiles:

| playerId | country | level |
|---|---|---|
| p-42  | ES | 18 |
| p-77  | SE | 6  |
| p-88  | ES | 10 |
| p-99  | SE | 11 |
| p-101 | FR | 20 |
| p-102 | DE | 9  |

---

## Design Decisions

### Architecture: layered

The project follows a **layered architecture**:

```
api/         → HTTP layer (controller, DTOs, error handling)
service/     → orchestration and business logic
repository/  → in-memory storage
domain/      → pure business model (no framework dependencies)
```

Dependencies point inward: `api` depends on `service`; `service` depends on
`repository` and `domain`; `repository` depends on `domain`; `domain` depends
on nothing.

**Why not hexagonal?** Hexagonal architecture (ports + adapters) shines in
projects with multiple data sources, many use cases, or a rich domain. Here
there are two endpoints, one use case, and two in-memory repositories.
Adding ports and adapters would introduce more indirection than benefit.
If the project grew, migrating to hexagonal would be a contained refactor.

### What is stored: the "recipe", not the "cake"

The assignment states that *"previously stored eligibility results must
reflect the current campaign rules when retrieved"*. This constrains the
storage model.

- **Storing the computed result** (`{p-42: eligible, p-77: not}`) would make
  it impossible to reflect future rule changes — once computed, we would no
  longer know why each player was marked the way they were.
- **Storing the recipe** (the campaign rules and the full list of player IDs)
  lets us **recalculate** eligibility on every read.

Therefore, `StoredEvaluation` holds:

- the `evaluationId`
- the `CampaignRules` used
- the **complete list of player IDs** submitted (eligible and non-eligible)
- the `evaluatedAt` timestamp

Eligibility itself is **never stored**. It is computed by the
`toResponse()` method on every POST and every GET.

### DTO vs. Domain separation

`CampaignRulesDto` and `CampaignRules` have the same fields but are kept
separate:

- The **DTO** uses `Integer minimumLevel` with `@NotNull @Min(1)`. A missing
  JSON field maps to `null` and triggers a 400. If the domain type were used
  directly in the request, a missing `minimumLevel` would default to `0` and
  silently make every player eligible.
- The **domain** uses primitive `int` because, by the time it is created,
  the value is guaranteed to be valid.
- Validation annotations (`jakarta.validation`) stay out of the domain.

The conversion is a single `toDomain()` method on the DTO.

### Idempotency and conflict handling

The assignment says *"Store an evaluation result once per evaluationId"* and
*"A campaign evaluation may be requested again with the same evaluationId"*.

Two behaviours are possible:

- **Strict**: reject any repeat with `409 Conflict`.
- **Idempotent**: return 200 if the body is identical, 409 if it differs.

I chose **idempotent**:

- The assignment explicitly acknowledges repeats, which suggests they should
  be handled, not just rejected.
- Idempotent APIs are safer for clients: retrying a POST after a network
  failure does not penalise them.

Implementation detail: comparison uses **rules + playerIds only**. The
`evaluatedAt` timestamp is deliberately excluded, since two requests with
identical bodies would otherwise always differ (they happen at different
instants).

### Unknown player IDs

If any submitted `playerId` is not present in the fixture, the request is
rejected with `400 Bad Request` **before anything is stored**. The response
lists **all** unknown IDs, not just the first one, so the client can fix
their payload in one go.

The alternative — evaluating the valid players and skipping the unknown
ones — was rejected because it silently hides a data problem.

### Concurrency

`CampaignEvaluationRepository` is backed by a `ConcurrentHashMap` and uses
`putIfAbsent` as a single atomic operation. A `containsKey + put` pattern
would open a race window between the check and the write.

The same applies to the "store once" guarantee: `putIfAbsent` ensures that
exactly one thread wins when multiple POSTs with the same `evaluationId`
arrive simultaneously.

### Validation

Bean Validation (Jakarta) is used at the DTO layer:

- `@NotBlank` on `evaluationId`.
- `@NotNull + @Valid` on the nested `campaignRules`.
- `@NotEmpty` on `playerIds` and `countries`, plus `@NotBlank` on their
  elements using container element syntax (`List<@NotBlank String>`).
- `@NotNull + @Min(1)` on `minimumLevel`.

Validation failures are mapped to `400 Bad Request` by
`GlobalExceptionHandler`.

### Exception handling

- Business exceptions (`UnknownPlayerException`, `EvaluationNotFoundException`,
  `EvaluationConflictException`) live in `service.exception` and are thrown
  by the service.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) and `ErrorResponse`
  live in `api.error` because they are HTTP concerns, not business ones.

---

## Ambiguities Identified

| # | Ambiguity | Decision | Rationale |
|---|---|---|---|
| 1 | Same `evaluationId` posted twice | 200 if body is identical, 409 if different | Assignment says "store once"; idempotency is the standard API pattern for repeats |
| 2 | "Current campaign rules" on retrieval | Store rules + player IDs, recalculate on every read | The only way to reflect rule changes without losing information |
| 3 | Unknown player IDs | Reject entire request with 400, listing all unknown IDs | Silent skipping hides a client-side data problem |
| 4 | Empty `playerIds` | 400 | A campaign evaluation without players is meaningless |
| 5 | Empty `countries` | 400 | A campaign without countries matches nobody |
| 6 | `minimumLevel < 1` | 400 | Levels are 1-based in the fixture |
| 7 | `evaluatedAt` in the idempotent response | Not exposed in the DTO | Keeps the contract simple; can be added if needed |
| 8 | Persistence across restarts | Not implemented (per assignment) | In-memory `ConcurrentHashMap` is sufficient |
| 9 | Duplicate IDs inside `playerIds` | Not deduplicated | Preserves order and avoids surprising the client |
| 10 | HTTP status for unknown evaluation | 404 Not Found | Standard REST semantics |

---

## Test Strategy

The suite follows a **test pyramid**:

### Unit tests (fast, no Spring)

- **`CampaignRulesTest`** — the eligibility rule: valid country, boundary
  level, invalid country, invalid level.
- **`PlayerRepositoryTest`** — fixture loads correctly, unknown IDs return
  `Optional.empty()`.
- **`CampaignEvaluationRepositoryTest`** — save-if-absent semantics,
  idempotency, and a **concurrency test** using `CountDownLatch` to release
  10 threads simultaneously and assert that exactly one stores the value.
- **`CampaignEvaluationServiceTest`** — service orchestration: eligibility
  for mixed players, unknown players, idempotent repeat, conflicting repeat,
  GET success and 404.

### Slice tests (web layer only)

- **`CampaignEvaluationControllerTest`** — uses
  `MockMvcBuilders.standaloneSetup` with a mocked `CampaignEvaluationService`
  and a manually registered `GlobalExceptionHandler`. Verifies HTTP status
  codes, JSON shape, and exception-to-response mapping in isolation.
  This runs in ~1.6s for all six tests without starting a Spring context.

### Integration tests (full context)

- **`CampaignEvaluationIntegrationTest`** — uses `@SpringBootTest` +
  `@AutoConfigureMockMvc`. Boots the full application and verifies the
  end-to-end flow (controller → service → repositories → fixture). Only two
  smoke tests are included, since the goal is to catch wiring regressions
  rather than to duplicate the slice tests.

### Note on container element validation paths

Bean Validation reports constraint violations on collection elements using
the `<list element>` path suffix. For example, `List<@NotBlank String>
playerIds` produces a field path of `playerIds[0].<list element>` rather
than `playerIds[0]`. Integration tests that assert on validation errors must
use the full path. This is standard Bean Validation 2.0+ behaviour; cleaning
it up would require post-processing the `Path` in `GlobalExceptionHandler`.

---

## What I Would Do Next

The following were deliberately left out of the current scope:

- **Campaigns as a first-class resource**: the current response returns
  only `campaignId`, referencing the campaign by identifier. A more
  scalable design would expose campaigns as their own resource
  (`GET /campaigns/{id}`, `PUT /campaigns/{id}/rules`) and let the
  evaluation reference them by ID. This avoids duplicating rule data in
  every evaluation response and follows standard REST resource
  normalisation. It was left out because the assignment models the
  campaign rules as part of the evaluation request, not as an
  independently managed resource.
- **Persistence**: swap the in-memory maps for JPA + H2/Postgres. The
  repository interfaces are already isolated, so the change would be
  contained.
- **Campaign rules update endpoint**: a `PUT /campaign-evaluations/{id}/rules`
  would make the "rules may change" scenario fully testable end-to-end.
- **Pagination** if `playerIds` lists can grow large.
- **Observability**: Spring Boot Actuator + Micrometer.
- **Rate limiting** per `evaluationId` to prevent abuse.
- **OpenAPI / Swagger UI** via `springdoc-openapi`.
- **Dockerfile** and docker-compose.

---

## AI Tooling Disclosure

See [AI_USAGE.md](./AI_USAGE.md) for details on how AI assistance was used
during this assignment.