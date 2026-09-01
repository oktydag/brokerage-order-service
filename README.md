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
- [The domain in one rule](#the-domain-in-one-rule)
- [Architecture](#architecture)
- [Concurrency](#concurrency)
- [Error model](#error-model)
- [Trade-offs and known limitations](#trade-offs-and-known-limitations)
- [Project layout](#project-layout)

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

### Run the packaged jar

```bash
java -jar target/brokerage-order-service-1.0.0.jar
```

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
  -X POST http://localhost:8080/api/v1/orders \
  -d '{"customerId":"CUST-1001","assetName":"THYAO","orderSide":"BUY","size":100,"price":300}'
```

Reserves the required balance and stores the order as `PENDING`. Returns `201` with a
`Location` header. `customerId` is required for an employee and ignored for a customer
credential, which may only act on its own account.

### List orders

```bash
curl -u admin:admin123 \
  'http://localhost:8080/api/v1/orders?customerId=CUST-1001&from=2026-01-01T00:00:00Z&to=2027-01-01T00:00:00Z'
```

Supported filters: `customerId`, `from`, `to`, `status` (repeatable), `assetName`,
`orderSide`, plus `page`, `size` and `sort`.

The date range is half-open — `from` inclusive, `to` exclusive — so consecutive ranges tile
without overlapping or dropping an order that lands exactly on a boundary. Both bounds are
optional; omitting one leaves that side unbounded.

### Cancel order

```bash
curl -u admin:admin123 -X DELETE http://localhost:8080/api/v1/orders/{orderId}
```

Only `PENDING` orders may be cancelled; anything else is refused with `409`. The reservation
returns to the customer's usable balance.

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
executed trade. Orders are settled **one transaction each**, so a single rejected order does
not abort the batch; the response reports the outcome per order:

```json
{
  "requested": 2,
  "matched": 1,
  "rejected": 1,
  "outcomes": [
    { "orderId": "…", "result": "MATCHED" },
    { "orderId": "…", "result": "REJECTED", "code": "ILLEGAL_ORDER_TRANSITION",
      "message": "Order … is CANCELED and cannot become MATCHED" }
  ]
}
```

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

On matching, the outgoing leg reduces only `size`: its `usableSize` was already removed when
the order was placed, so deducting it twice would double-charge the customer.

Both legs of an order are derived in exactly one place — `OrderSide` — so the amount reserved
at placement, released on cancellation and settled on matching cannot drift apart.

---

## Architecture

### Tactical DDD

The invariant above is the service's only real risk, and DDD's tactical patterns exist
precisely to make an invariant unbreakable from outside. That is why they are used here, and
why the strategic half is not: there is one bounded context, and context-mapping ceremony over
a single context would be decoration.

**Rich entities, no anemic model.** `Asset.reserve()`, `Asset.credit()`, `Order.cancel()` and
`Order.match()` enforce their own rules. There are no balance or status setters, so no caller —
present or future — can move a balance or a lifecycle state without passing through the checks.

**Value objects.** `Amount` (BigDecimal at fixed scale, never floating point), `CustomerId` and
`AssetName`. Typed identifiers make the classic transposition bug in calls like
`reserve(customerId, assetName)` a compile error rather than a support ticket.

**Ubiquitous language.** The specification's vocabulary is kept verbatim — `usableSize`, `size`,
`orderSide`, `createDate`, `PENDING`/`MATCHED`/`CANCELED`. Renaming `usableSize` to something
more "domain-like" would lose the shared language, which is the point of having one.

### The aggregate boundary is the lock boundary

`Portfolio` — one customer's complete set of balances — is the aggregate root. `Asset` is an
entity inside it, and all of its mutators are package-private: balances move only through the
root.

That boundary was chosen because it is the **transactional consistency boundary**: placing an
order touches the reserved asset and, on settlement, the counter asset, and those must move
together or not at all. It is also the **locking granularity** — `PortfolioRepository`
`SELECT … FOR UPDATE`s exactly these rows. Aggregate, transaction and lock are deliberately
the same line.

`Order` is a separate aggregate referenced by id. Folding an unbounded, ever-growing order
history into the portfolio would mean loading a customer's entire trading record to reserve a
single amount. The two aggregates are therefore committed in one transaction with the
portfolio lock providing atomicity — a conscious, documented departure from the
one-aggregate-per-transaction guideline, taken because the alternative is unusable.

### Modular monolith, not microservices

Order placement is one atomic operation: create the order and reserve the balance. Splitting
`Order` and `Asset` into separate services would replace an ACID transaction with a saga, an
outbox, idempotency keys and a reconciliation job for half-reserved orders — and would buy
nothing here: no independent scaling need, no separate deploy cadence, no second team. That is
a distributed monolith with extra failure modes.

The module boundaries are drawn where the seams would be if that ever changed:

| Module | Would split off when |
|---|---|
| `matching` | First. Different load profile and latency budget; scales independently |
| `asset` (ledger) | Next. The heart of the system; separate SLA and audit requirements |
| `order` | Stays with the ledger — same transaction, most expensive to separate |
| Read/reporting | Cheap and early: read-only, no writer to break |

Domain events (`OrderPlaced`, `OrderCanceled`, `OrderMatched`) are published through Spring's
in-process publisher. They need no infrastructure today and keep the seam visible: moving to a
broker later changes the publisher, not the domain.

### CQRS at the code level only

Every command and every query is a record, handled by exactly one class implementing
`CommandHandler<C, R>` or `QueryHandler<Q, R>`. Adding an operation means adding a command or
query and its handler; nothing existing is edited, so handlers stay small and single-purpose
instead of accumulating into a service that does everything.

| Command | Handler |
|---|---|
| `PlaceOrderCommand` | `PlaceOrderHandler` |
| `CancelOrderCommand` | `CancelOrderHandler` |
| `MatchOrderCommand` | `MatchOrderHandler` |
| `MatchOrdersCommand` | `MatchOrdersHandler` |

| Query | Handler |
|---|---|
| `ListOrdersQuery` | `ListOrdersHandler` |
| `GetOrderQuery` | `GetOrderHandler` |
| `ListAssetsQuery` | `ListAssetsHandler` |

Handlers are injected directly rather than dispatched through a bus. Direct injection keeps
the call type-safe and traceable in an IDE; a `CommandBus` resolving handlers by generic type
is a single class to add later if cross-cutting behaviour ever needs one place to live.

Command and query sides use distinct repositories, and query handlers run in `readOnly`
transactions, which disables Hibernate dirty-check snapshotting on listing endpoints.

A handler contains no business rules. It loads aggregates, calls them, saves and publishes —
nothing more. There is no domain service between the handler and the model: `Order` decides
what an order reserves and which lifecycle transitions are legal, and `Portfolio` decides how
a balance moves. `portfolio.reserve(order.reservation())` is the whole of order placement's
balance logic, and both halves of it live in the domain.

They are **not** separated in infrastructure. A single database serves both, because read and
write shapes here are near-identical (`List Orders` is the order table; `List Assets` is the
asset table) and there is no denormalisation to gain. Splitting the store would add eventual
consistency and a read-your-own-writes problem in exchange for nothing.

Under real load, the first step would not be a read store but a **connection-pool bulkhead** —
a reserved pool for the write path so a burst of reporting queries cannot starve order entry —
followed by read-replica routing. Both are single-point changes because the code split already
exists. The system's actual scale limit is neither: it is that one customer's balance updates
must serialise, which is a correctness requirement, not a bottleneck to optimise away.

---

## Concurrency

Two simultaneous BUY orders must not both see a sufficient balance and both succeed. The
defence has four layers:

1. **Pessimistic write lock** over the customer's asset rows, taken *before* the balance is
   read. Check-then-lock would leave exactly the race it is meant to close.
2. **Deterministic lock ordering** — orders row first, then asset rows sorted by asset name —
   so overlapping transactions queue instead of deadlocking.
3. **Optimistic `@Version`** on both entities, guarding any future code path that reaches a
   row without taking the portfolio lock.
4. **Database `CHECK` constraints** (`usable_size >= 0`, `usable_size <= size`). A corrupted
   balance is unrecoverable without an audit trail, so the invariant is also enforced where no
   application bug can reach.

Lock waits are bounded (`jakarta.persistence.lock.timeout`), and lock or version conflicts
surface as `409` with a retryable code rather than a generic `500`.

Verified behaviour: ten concurrent BUY orders of 10 000 TRY each against a 75 000 TRY balance
produce exactly seven `201`s and three `422`s, with no overdraft.

### Virtual threads

`spring.threads.virtual.enabled` is on. This is safe on Java 24 and later, where
[JEP 491](https://openjdk.org/jeps/491) removed the carrier-thread pinning that used to make
blocking JDBC calls a poor fit for virtual threads.

---

## Error model

All errors are RFC 7807 `application/problem+json` with a stable machine-readable `code`.
Status codes are chosen to tell the client what to do next:

| Status | Meaning | Examples |
|---|---|---|
| `400` | The request is malformed | `VALIDATION_FAILED`, `INVALID_ORDER` |
| `401` / `403` | Not authenticated / out of scope | `UNAUTHENTICATED`, `FORBIDDEN` |
| `404` | No such record | `ORDER_NOT_FOUND` |
| `409` | Valid request, resource moved on — a retry may work | `ILLEGAL_ORDER_TRANSITION`, `CONCURRENT_MODIFICATION` |
| `422` | Understood and permanently refused by a business rule | `INSUFFICIENT_USABLE_BALANCE` |

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

## Security

HTTP Basic over a stateless API, BCrypt-hashed credentials, roles `ADMIN` and `CUSTOMER`.

The customer-scoping rule (Bonus 1) is enforced structurally rather than by remembering a
check. `AccessPolicy` is the only code that reads the security context; it produces an
`AccessScope` value that must be passed into the application layer:

- an **employee** gets an unrestricted scope and must name a customer;
- a **customer** gets a scope pinned to their own id — a `customerId` in the request naming
  anyone else is rejected with `403`, and one naming themselves is redundant.

Because the scope is a parameter rather than an ambient lookup, an endpoint cannot silently
forget authorisation, and the rule is exercisable without an authenticated request. Operator
endpoints live under `/api/v1/admin/**`, which the filter chain gates on the `ADMIN` role, so
authorisation cannot be lost by omitting an annotation on a new method.

CSRF protection is disabled deliberately: it defends against a browser attaching ambient
credentials to a forged request, and there are none here — no session, no cookie, every call
carries its own `Authorization` header.

---

## Trade-offs and known limitations

**"Delete order" cancels rather than deletes.** The specification calls the endpoint delete but
describes cancellation. Orders are financial records; they are kept in `CANCELED` status and no
row is ever removed. An order that exists but is no longer pending answers `409`, not `404`, so
the caller can tell "no such order" from "too late".

**Matching has no order book.** Bonus 2 asks for an operator endpoint that marks a set of
pending orders as executed, and that is what is implemented. There is no counterparty
matching, no price-time priority and no partial fills — each order settles in full at its own
limit price. A real engine would match opposing orders against each other; that is a different
component with a different design, deliberately out of scope.

**The domain model carries JPA annotations.** A separate persistence model with mappers would
isolate the domain from the ORM. At two entities the mapping cost exceeds the benefit and it
would hurt the readability the exercise asks for. This is the first thing to change if the
model grows.

**Queries load entities.** Dynamic filtering uses criteria specifications, which need entities
rather than a narrower projection. `readOnly` transactions remove the cost that matters most
(dirty-check snapshots). A fixed-shape, high-volume report would justify a constructor
projection instead.

**Date bounds are optional.** The specification says "within a specified date range". Both
bounds are accepted but neither is required, since refusing to list a customer's orders without
a date range is a worse default than an unbounded — but always paginated — result.

**Every customer needs a TRY row.** Because every order settles against TRY, the TRY row is the
de-facto lock anchor that serialises all of a customer's portfolio mutations. Seeded customers
get one. A customer created with no TRY row at all cannot place orders, and customer onboarding
is outside the scope of this exercise.

**Single connection pool.** As argued above, the read/write split stops at the code level. The
bulkhead is described, not implemented, because it would be unexercised complexity at
evaluation scale.

---

## Project layout

```
com.brokerage
├── common
│   ├── domain          Amount, CustomerId, AssetName, Reservation, Settlement,
│   │                   AccessScope, exception base
│   ├── application     CommandHandler, QueryHandler
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
│   ├── domain          Order (aggregate root), OrderSide, lifecycle, events
│   ├── application
│   │   ├── command     PlaceOrder, CancelOrder + handlers
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
