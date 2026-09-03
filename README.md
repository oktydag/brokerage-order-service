# Brokerage Order Service

A backend API that lets brokerage employees submit, list and cancel stock orders on behalf of
their customers, and lets an operator execute pending orders. Every order moves through the
lifecycle `PENDING → MATCHED | CANCELED`.

Built with **Java 25** and **Spring Boot 3.5**, persisting to an in-memory **H2** database.

---

## Contents

- [Quick start](#quick-start)
- [Demo data](#demo-data)
- [API](#api)
- [Architecture](#architecture)
- [Architecture decision records](#architecture-decision-records)
- [The domain in one rule](#the-domain-in-one-rule)
- [Concurrency](#concurrency)
- [Idempotency](#idempotency)
- [Security](#security)
- [Error model](#error-model)
- [Testing strategy](#testing-strategy)
- [Build and deployment](#build-and-deployment)
- [Known limitations](#known-limitations)
- [AI-assisted development](#ai-assisted-development)
- [Known limitations](#known-limitations)

---

## Quick start

Requires **JDK 25**. Maven is not needed — the wrapper downloads it.

```bash
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080` with the schema migrated and demo data loaded.

| What | Where |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI document | http://localhost:8080/v3/api-docs |

### Build

```bash
./mvnw clean package
```

### Test

```bash
./mvnw test
```

### Test with coverage enforcement

```bash
./mvnw clean verify
```

JaCoCo fails the build below 90% instruction coverage. The report lands in
`target/site/jacoco/index.html`.

### Run the packaged jar

```bash
java -jar target/brokerage-order-service-1.0.0.jar
```

### Run in Docker

```bash
docker compose up --build
```

The service comes up on the same port with the same demo data. Compose passes a container
healthcheck against `/actuator/health/readiness`, so `docker compose ps` shows `healthy` only
once the application is actually serving.

### Disable the seed data

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.demo-data.enabled=false
```

---

## Demo data

All endpoints require **HTTP Basic** authentication. These credentials are development values
declared in `application.yml`, not secrets.

| Username | Password | Role | Customer |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | — (may act on any customer) |
| `alice` | `alice123` | CUSTOMER | `CUST-1001` |
| `bob` | `bob123` | CUSTOMER | `CUST-1002` |

Opening balances:

| Customer | TRY | Holdings |
|---|---|---|
| `CUST-1001` | 250 000 | THYAO 500, ASELS 300 |
| `CUST-1002` | 75 000 | GARAN 1 000 |

---

## API

### Create order

```bash
curl -u admin:admin123 -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 3f9a1c7e-2b44-4c0f-9a6d-8e1b5c2d7f01' \
  -X POST http://localhost:8080/api/v1/orders \
  -d '{"customerId":"CUST-1001","assetName":"THYAO","orderSide":"BUY","size":100,"price":300}'
```

Reserves the required balance and stores the order as `PENDING`. Returns `201` with a
`Location` header. `customerId` is required for an employee and ignored for a customer
credential, which may only act on its own account. The `Idempotency-Key` header is optional and
described under [Idempotency](#idempotency).

### List orders

```bash
curl -u admin:admin123 \
  'http://localhost:8080/api/v1/orders?customerId=CUST-1001&from=2026-01-01T00:00:00Z&to=2027-01-01T00:00:00Z'
```

Filters: `customerId`, `from`, `to`, `status` (repeatable), `assetName`, `orderSide`, plus
`page`, `size` and `sort`.

The date range is half-open — `from` inclusive, `to` exclusive — so consecutive ranges tile
without overlapping or dropping an order that lands exactly on a boundary. Both bounds are
optional; omitting one leaves that side unbounded.

### Cancel order

```bash
curl -u admin:admin123 -X DELETE http://localhost:8080/api/v1/orders/{orderId}
```

Releases the reservation back to the customer's usable balance. Cancelling an order that is
already `CANCELED` returns `200` and the same view; only a `MATCHED` order is refused, with
`409`.

The order is never deleted. The specification names this endpoint "Delete Order" but describes
cancellation, and orders are financial records: a deleted row cannot be audited, reconciled, or
explained to a customer asking what happened to their money. The order survives in `CANCELED`
status and no row is ever removed. An order that exists but is no longer pending answers `409`
rather than `404`, because "no such order" and "too late" are different problems for the
caller.

### List assets

```bash
curl -u alice:alice123 'http://localhost:8080/api/v1/assets?nonZeroOnly=true'
```

Returns `size`, `usableSize` and `reservedSize` (their difference) per holding. Filters:
`customerId`, `assetName`, `nonZeroOnly`.

### Match orders (operator only)

```bash
curl -u admin:admin123 -H 'Content-Type: application/json' \
  -X POST http://localhost:8080/api/v1/admin/orders/match \
  -d '{"orderIds":["<id1>","<id2>"]}'
```

Each order transitions `PENDING → MATCHED` and both asset balances are updated to reflect the
executed trade. Orders are settled **one transaction each**, so a single rejected order does not
abort the batch:

```json
{
  "requested": 3,
  "matched": 1,
  "alreadyMatched": 1,
  "rejected": 1,
  "outcomes": [
    { "orderId": "…", "result": "MATCHED" },
    { "orderId": "…", "result": "ALREADY_MATCHED" },
    { "orderId": "…", "result": "REJECTED", "code": "ILLEGAL_ORDER_TRANSITION",
      "message": "Order … is CANCELED and cannot become MATCHED" }
  ]
}
```

Re-sending a batch is safe: orders already executed come back as `ALREADY_MATCHED` and are not
settled twice.

---

## Architecture

A **modular monolith**. One deployable, one database, one transaction around the operation that
matters — creating an order and reserving the balance it needs. The reasoning is recorded in
[ADR-0001](docs/adr/ADR-0001-Use-a-Modular-Monolith-Instead-of-Microservices.md).

The seams are prepared rather than used. Domain events (`OrderPlaced`, `OrderCanceled`,
`OrderMatched`) are published through Spring's in-process publisher, so moving to a broker later
changes the publisher and not the domain. Were the system ever to justify splitting, the order
would be: matching first (different load profile and latency budget), then the ledger (different
SLA and audit requirements), with order management staying alongside the ledger because it
shares the transaction.

### Modules

Four business modules plus shared plumbing. Each owns its own vertical slice rather than
contributing to shared technical layers.

| Module | Responsibility |
|---|---|
| `order` | The order aggregate, its lifecycle, placement and cancellation |
| `asset` | The portfolio aggregate: balances, reservations, settlement |
| `matching` | Operator-driven execution of pending orders |
| `security` | Authentication and the access scope every operation is evaluated against |
| `common` | Value objects, handler contracts, idempotency, error handling, configuration |

### Layers within a module

```
web              Controllers. Translate HTTP, resolve the access scope, nothing else.
  ↓
application      One handler per command or query. Loads, calls, saves, publishes.
  ↓
domain           Aggregates, value objects, repository interfaces. All the rules.
  ↑
infrastructure   Spring Data repositories, locking queries, criteria specifications.
```

Dependencies point inward. `domain` declares the repository interfaces it needs;
`infrastructure` implements them.

`Order` and `Asset` carry JPA annotations directly rather than being mapped to separate
persistence entities. The orthodox arrangement — plain domain objects, separate entities,
mappers between them — would mean three models and two mappers for two entities, which costs
more readability than the isolation is worth here. Value objects stay out of the ORM's way
behind `AttributeConverter`s, so `Amount`, `CustomerId`, `AssetName` and the rest remain pure.
The coupling in practice is annotations plus a protected no-arg constructor; the rules
themselves have no framework dependency. This is the first thing to revisit if the model grows,
and splitting later is mechanical, because the domain already depends on its own repository
interfaces rather than on Spring Data directly.

### Request flow

Placing an order, end to end:

```
POST /api/v1/orders
  │
  ├─ OrderController          resolves the target customer from the credential,
  │                           never from the request body
  ├─ PlaceOrderHandler        entry point for the command
  ├─ PlaceOrderIdempotency    replays a stored result, or lets the placement run
  └─ OrderPlacement           @Transactional — one unit of work
       ├─ Order.place(…)              domain validates and creates the PENDING order
       ├─ IdempotencyClaims.claim(…)  unique constraint, same transaction
       ├─ portfolios.lockForUpdate(…) SELECT … FOR UPDATE over the customer's rows
       ├─ portfolio.reserve(…)        domain moves the usable balance
       └─ publish OrderPlaced
```

### The aggregate boundary

`Portfolio` — the complete set of one customer's balances — is the aggregate root. `Asset` is an
entity inside it, and all of its mutators are package-private, so balances can only move through
the root.

That boundary was chosen because it is the transactional consistency boundary: placing an order
touches the reserved asset and, on settlement, the counter asset, and those must move together
or not at all. It is also the locking granularity — `PortfolioRepository.lockForUpdate` locks
exactly these rows. **Aggregate, transaction and lock are deliberately the same line.**

`Order` is a separate aggregate referenced by id. Folding an unbounded, ever-growing order
history into the portfolio would mean loading a customer's entire trading record to reserve a
single amount. The two aggregates are therefore committed in one transaction with the portfolio
lock providing atomicity — a conscious departure from the one-aggregate-per-transaction
guideline, taken because the alternative is unusable.

Because every order settles against TRY, the TRY row is the de-facto anchor that serialises all
of a customer's balance changes.

### Tactical DDD, no domain services

The rules live on the model. `Asset.reserve`, `Asset.credit`, `Order.cancel` and `Order.match`
enforce their own preconditions, and there are no balance or status setters. There is **no
domain service** between the application handler and the model: a handler loads aggregates,
calls them, saves and publishes, and nothing else. None of the four command handlers contains a
conditional, which is the check that no rule has leaked upward.

Strategic DDD is deliberately absent — there is one bounded context, and context-mapping
ceremony over a single context would be decoration.

### Commands and queries

Commands and queries are separated in code but share one database. The read model here is nearly
identical to the write model — listing orders is the order table, listing assets is the asset
table — so there is no denormalisation to gain and no read store to justify. Query handlers run
in `readOnly` transactions, which disables Hibernate's dirty-check snapshotting on listing paths.

Under real load the first step would be a connection-pool bulkhead — a reserved pool for the
write path so a burst of reporting cannot starve order entry — then read-replica routing. Both
are single-point changes because the code split already exists.

Every operation is a record with exactly one handler implementing `CommandHandler<C, R>` or
`QueryHandler<Q, R>`:

| Command | Handler | Query | Handler |
|---|---|---|---|
| `PlaceOrderCommand` | `PlaceOrderHandler` | `ListOrdersQuery` | `ListOrdersHandler` |
| `CancelOrderCommand` | `CancelOrderHandler` | `GetOrderQuery` | `GetOrderHandler` |
| `MatchOrderCommand` | `MatchOrderHandler` | `ListAssetsQuery` | `ListAssetsHandler` |
| `MatchOrdersCommand` | `MatchOrdersHandler` | | |

Adding an operation means adding a record and a handler. Nothing existing is edited, and there
is no registry to update — Spring discovers handlers by annotation. Handlers are injected
directly rather than dispatched through a bus, which keeps calls type-safe and traceable in an
IDE; a `CommandBus` resolving handlers by generic type is one class to add later if
cross-cutting behaviour ever needs a single place to live.

### Technology choices

Java 25 on Spring Boot 3.5.x. Boot 4.x is available and targets Java 25, but brings Jackson 3
and Spring Security 7 migration with it, which buys nothing for a service of this size.

Virtual threads are enabled (`spring.threads.virtual.enabled`). This is safe here because Java 24 and later removed the need for explicit
virtual-thread configuration for the relevant executor behavior.

### Project layout

```
com.brokerage
├── common
│   ├── domain
│   │   └── valueobjects  Amount, CustomerId, AssetName, Reservation, Settlement,
│   │                     AccessScope, IdempotencyKey, RequestFingerprint
│   ├── application     CommandHandler, QueryHandler
│   ├── idempotency     Claim record, repository, retention cleaner
│   ├── jpa             Value-object attribute converters
│   ├── web             RFC 7807 handling, pagination envelope
│   └── config          Clock, OpenAPI, demo-data seeding
├── asset
│   ├── domain          Portfolio (aggregate root), Asset, PortfolioRepository
│   ├── application
│   │   └── query       ListAssetsQuery + handler, read model
│   ├── infrastructure  JPA repositories, locking, specifications
│   └── web             AssetController
├── order
│   ├── domain          Order (aggregate root), repository, events
│   │   └── valueobjects  OrderSide, OrderStatus
│   ├── application
│   │   ├── command     PlaceOrder, CancelOrder + handlers, placement,
│   │   │               idempotency policy
│   │   └── query       ListOrders, GetOrder + handlers
│   ├── infrastructure  Query repository, specifications
│   └── web             OrderController
├── matching
│   ├── application
│   │   └── command     MatchOrder, MatchOrders + handlers, per-order report
│   └── web             MatchingController
└── security            AccessPolicy, principal, user store, filter chain
```

Schema migrations live in `src/main/resources/db/migration`. Flyway owns the schema; Hibernate
runs with `ddl-auto: validate` and fails startup if the mapping and the migration disagree.

---

## Architecture decision records

The two decisions with the widest blast radius are recorded separately in
[`docs/adr`](docs/adr), each with the situation that forced it and the reasoning behind it.
The rest of the design rationale is inline in this document, next to the thing it explains.

| # | Decision | Status |
|---|---|---|
| [ADR-0001](docs/adr/ADR-0001-Use-a-Modular-Monolith-Instead-of-Microservices.md) | Use a modular monolith instead of microservices | Accepted |
| [ADR-0002](docs/adr/ADR-0002-Idempotent-Order-Creation.md) | Idempotent order creation | Accepted |

---

## The domain in one rule

TRY is not a separate table. It is an ordinary row in the asset table, and every order is
denominated against it — which makes `usableSize` a reservation ledger rather than a plain
column. The single invariant the whole service exists to protect is:

```
0 ≤ usableSize ≤ size
size − usableSize = total reserved by that customer's open PENDING orders on that asset
```

`size` is everything the customer owns. `usableSize` is the part not already committed to a
pending order. Every operation is a movement between the two:

| Operation | BUY | SELL |
|---|---|---|
| Place | `TRY.usableSize −= size × price` | `X.usableSize −= size` |
| Cancel | `TRY.usableSize += size × price` | `X.usableSize += size` |
| Match | `TRY.size −= size × price`<br>`X.size += size`, `X.usableSize += size` | `X.size −= size`<br>`TRY.size += size × price`, `TRY.usableSize += size × price` |

On matching, the outgoing leg reduces only `size`: its `usableSize` was already removed when the
order was placed, so deducting it twice would double-charge the customer.

Both legs of an order are derived in exactly one place — `OrderSide` — so the amount reserved at
placement, released on cancellation and settled on matching cannot drift apart.

---

## Concurrency

Two simultaneous BUY orders must not both see a sufficient balance and both succeed. Reading the
balance and *then* locking leaves exactly the race the lock is meant to close, so the defence is
built in four layers:

1. **Pessimistic write lock** over the customer's asset rows, taken *before* the balance is read
   for a decision
2. **Deterministic lock ordering** — the order row first, then asset rows sorted by asset name —
   which prevents circular-wait deadlocks for these application-managed locks
3. **Optimistic `@Version`** on both entities, guarding any future path that reaches a row
   without taking the portfolio lock
4. **Database `CHECK` constraints** (`usable_size >= 0`, `usable_size <= size`). These are not
   redundant with the aggregate: a corrupted balance is unrecoverable without an audit trail, so
   the invariant is also enforced where no application bug can reach it

Lock waits are bounded by `jakarta.persistence.lock.timeout`, and lock or version conflicts
surface as `409` with a retryable code rather than a generic `500`.

A customer's balance changes serialise as a result. That is a correctness requirement, not a
bottleneck to optimise away, and it does not affect throughput across customers, which
parallelises freely.

Optimistic locking alone was rejected: it would detect the conflict only after the work is done,
and would push retry logic into every caller of a financial write.

---

## Idempotency

Every write path is safe to retry, by two different mechanisms.

Where the client names the resource, the lifecycle converges on its terminal state.
`Order.cancel` and `Order.match` return an `Optional` describing the balance movement to apply,
empty when the order is already in the target state — so the domain decides, and the handler
simply applies whatever it is handed:

| Endpoint | Retry behaviour |
|---|---|
| `DELETE /orders/{id}` | Already `CANCELED` → `200`, same view. Only `MATCHED` conflicts (`409`) |
| `POST /admin/orders/match` | Already `MATCHED` → `ALREADY_MATCHED`, never settled twice |

A retry that reaches the same terminal state has achieved the caller's intent, so it succeeds; a
retry that conflicts with a *different* terminal state is a genuine conflict and gets `409`.

Order placement is the exception: it *creates* the identity, so there is nothing to converge on
and it takes a client-generated key instead. The decision is recorded in
[ADR-0002](docs/adr/ADR-0002-Idempotent-Order-Creation.md).

| Situation | Response |
|---|---|
| First request | `201`, `Idempotency-Replayed: false` |
| Retry, same payload | `200`, `Idempotency-Replayed: true`, the original order |
| Same key, different payload | `422 IDEMPOTENCY_KEY_REUSE` |
| No key sent | Not deduplicated — every request creates an order |

The claim is written inside the placement transaction against
`UNIQUE (customer_id, idempotency_key)`, so claim and order commit together and a concurrent
duplicate blocks on the index rather than needing an `IN_PROGRESS` state — and therefore needs
no reaper for claims left behind by a crashed process. If placement fails, the claim rolls back
with it, so a genuine retry is free to execute.

The fingerprint is a SHA-256 over normalised domain values rather than the raw request body, so
`100` and `100.00` are the same request; parts are length-prefixed before hashing so a separator
cannot be moved between them. Only successful placements are recorded: a rejected order
committed nothing, so re-running it is harmless and produces the same rejection.

Claims are purged after `app.idempotency.retention` (default 24 hours). That window is exactly
how long a retry stays safe, so it is a contract with clients rather than housekeeping.

---

## Security

HTTP Basic over a stateless API, BCrypt-hashed credentials, roles `ADMIN` and `CUSTOMER`.

The customer-scoping rule is enforced structurally rather than by remembering a check.
`AccessPolicy` is the only code that reads the security context; it produces an `AccessScope`
value that must be passed into the application layer:

- an **employee** gets an unrestricted scope and must name a customer;
- a **customer** gets a scope pinned to their own id — a `customerId` naming anyone else is
  rejected with `403`.

Because the scope is a parameter rather than an ambient lookup, an endpoint cannot silently
forget authorisation, and the rule is exercisable without an authenticated request. Operator
endpoints live under `/api/v1/admin/**`, which the filter chain gates on the `ADMIN` role, so
authorisation cannot be lost by omitting an annotation on a new method.

CSRF protection is disabled deliberately: it defends against a browser attaching ambient
credentials to a forged request, and there are none here — no session, no cookie, every call
carries its own `Authorization` header.

---

## Error model

All errors are RFC 7807 `application/problem+json` with a stable machine-readable `code`.
Status codes are chosen to tell the client what to do next:

| Status | Meaning | Examples |
|---|---|---|
| `400` | The request is malformed | `VALIDATION_FAILED`, `INVALID_ORDER` |
| `401` / `403` | Not authenticated / out of scope | `UNAUTHENTICATED`, `FORBIDDEN` |
| `404` | No such record | `ORDER_NOT_FOUND` |
| `409` | Valid request, resource moved on — a retry may work | `ILLEGAL_ORDER_TRANSITION`, `CONCURRENT_MODIFICATION`, `DUPLICATE_REQUEST` |
| `422` | Understood and permanently refused by a business rule | `INSUFFICIENT_USABLE_BALANCE`, `IDEMPOTENCY_KEY_REUSE` |

```json
{
  "type": "https://api.brokerage.com/problems/insufficient_usable_balance",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "Customer CUST-1001 has 220000 usable TRY but 3000000 is required",
  "code": "INSUFFICIENT_USABLE_BALANCE",
  "assetName": "TRY",
  "required": 3000000,
  "available": 220000
}
```

---

## Testing strategy

186 tests, all run by `./mvnw test`. Coverage is **99.6% of instructions** and **98.1% of
branches**; `verify` fails the build under 90%.

| Kind | Named | Count | What it proves |
|---|---|---|---|
| Unit | `*Test` | 140 | Domain rules in isolation, handlers against mocks |
| Integration | `*IntegrationTest` | 38 | The real stack: HTTP, security, Flyway schema, JPA, transactions |
| Concurrency | `*ConcurrencyTest` | 8 | Invariants hold when requests collide |

### Unit tests

The weight sits on the domain, where the rules are. `AssetTest` and `PortfolioTest` drive the
reservation ledger directly, from inside the aggregate's own package so they exercise the
package-private mutators the way `Portfolio` does. `OrderTest` covers the lifecycle. Handlers
are tested against mocked repositories — they should contain no rules, and these tests fail if
one appears.

### Integration tests

Full Spring context and MockMvc. Each test provisions its own customer, so nothing depends on
execution order or on the demo seed, and assertions go through the aggregate to the database
rather than stopping at the response body — which is what catches a balance that moved when it
should not have.

### Concurrency tests

Real threads, real transactions, released together from a common start line by
`src/test/java/com/brokerage/support/ConcurrentRuns.java`.

---

## Build and deployment

### Health endpoints

Actuator exposes `health` and `info` only. Three paths are reachable without authentication
because container and cluster probes need them; everything else under `/actuator` requires an
authenticated `ADMIN`:

| Path | Used by |
|---|---|
| `/actuator/health` | Humans and the Docker healthcheck |
| `/actuator/health/liveness` | Kubernetes `startupProbe` and `livenessProbe` |
| `/actuator/health/readiness` | Kubernetes `readinessProbe`, Compose healthcheck |

`show-details` is `never`, so the probes report status without leaking component detail.

### Container image

A two-stage `Dockerfile`: Maven on Temurin 25 builds the jar, `eclipse-temurin:25-jre-alpine`
runs it. The runtime layer is hardened for the cluster's `runAsNonRoot` policy:

- runs as **numeric** UID/GID `10001:10001` — a named user would make kubelet unable to verify
  the image is non-root and the pod would fail to start
- `JAVA_OPTS` defaults to `-XX:MaxRAMPercentage=75.0`, so the heap follows the container limit
  rather than the host's memory
- `exec java …` as the entrypoint, so the JVM is PID 1's child and receives `SIGTERM` directly

### Pipeline

`.gitlab-ci.yml`, five stages:

| Stage | Job | What runs |
|---|---|---|
| `build` | `build` | `mvn compile` |
| `unit-test` | `unit-test` | Unit tests only, via `-Dtest='!*IntegrationTest,!*ConcurrencyTest'` |
| `integration-test` | `integration-test` | `mvn verify` — the full suite plus the 90% JaCoCo gate, with the total reported back to GitLab as the pipeline coverage figure |
| `package` | `build-image` | Builds and pushes the image, tagged with the commit SHA, plus `latest` on the default branch |
| `deploy` | `deploy-stage`, `deploy-production` | `kustomize edit set image` then `kubectl apply -k`, waiting on `kubectl rollout status` |

The image is built **after** the tests pass, so a failing build never publishes an artifact.
Stage deploys automatically on the default branch; production is the same job gated behind a
manual trigger.

### Kubernetes

`.deploy` is a Kustomize tree — a `base` plus a `stage` and a `production` overlay:

```
.deploy
├── base                 ServiceAccount, ConfigMap, Secret, Deployment, Service
└── overlays
    ├── stage            namespace brokerage-stage, ingress, 1 replica, DEBUG logging
    └── production       namespace brokerage-production, ingress, 2 replicas,
                         HPA (2–6 pods on CPU/memory), pod anti-affinity
```

```bash
kubectl apply -k .deploy/overlays/stage
```

The deployment runs read-only-rootfs with all capabilities dropped, `RuntimeDefault` seccomp and
an `emptyDir` mounted at `/tmp` for the JVM's temporary directory. Rollouts are surge-only
(`maxUnavailable: 0`), and a `preStop` sleep plus a 40-second grace period give the ingress time
to stop routing before the JVM shuts down.

**Before a real deployment**, three placeholders have to change: the image registry in each
overlay's `kustomization.yaml`, the ingress hostnames, and the `CHANGEME` values in the Secret.
The manifests deploy the service exactly as the exercise specifies it — in-memory H2 and
config-seeded demo credentials — so they are runnable and honest rather than implying a database
tier the application does not have. Wiring a real datastore means adding the JDBC driver, adding
`SPRING_DATASOURCE_*` to the Secret, and pointing Flyway at it; nothing in the code assumes H2.

---

## Known limitations

**Matching has no order book.** The optional extension asks for an operator endpoint that marks
a set of pending orders as executed, and that is what is implemented. There is no counterparty
matching, no price-time priority and no partial fills — each order settles in full at its own
limit price. A real engine would match opposing orders against each other; that is a different
component with a different design, deliberately out of scope.

**Date bounds are optional.** The specification says "within a specified date range". Both
bounds are accepted but neither is required, since refusing to list a customer's orders without
a date range is a worse default than an unbounded — but always paginated — result.

**Every customer needs a TRY row.** Because every order settles against TRY, the TRY row is the
de-facto lock anchor that serialises a customer's portfolio mutations. Seeded customers get one.
A customer created with no TRY row cannot place orders, and customer onboarding is outside the
scope of this exercise.

**Domain events have no subscribers.** `OrderPlaced`, `OrderCanceled` and `OrderMatched` are
published through Spring's in-process publisher, but nothing consumes them yet. They exist as a
seam, not a feature. Anything with side effects should subscribe with
`@TransactionalEventListener` so it runs after commit; and an event can still be lost if the
process dies between commit and delivery, which is what an outbox would fix.

**Single connection pool.** The read/write split stops at the code level. The bulkhead described
under [Commands and queries](#commands-and-queries) is documented, not implemented, because it
would be unexercised complexity at evaluation scale.

**Persistence is in-memory.** H2 with `DB_CLOSE_DELAY=-1`, as the specification allows. Nothing
in the code depends on H2 specifically, but it does mean a restarted pod starts with an empty
book, and that two replicas do not share state. The production overlay's replica count and HPA
are therefore aspirational until a shared datastore is wired in.

**No PodDisruptionBudget.** Voluntary disruptions can take the last pod down. Worth adding
alongside the HPA once the service is genuinely stateless behind a shared database.

---

## AI-assisted development

AI coding tools were used throughout the development of this project as an
engineering assistant.

AI was primarily used for:

- Reviewing unit, integration and concurrency test scenarios
- Identifying potential edge cases and concurrency issues
- Assisting with refactoring and repetitive implementation work
- Reviewing code for potential bugs and over-engineering
- Preparing architecture diagrams and reviewing architectural documents

The goal was to use AI to improve development speed and exploration while
keeping technical ownership.