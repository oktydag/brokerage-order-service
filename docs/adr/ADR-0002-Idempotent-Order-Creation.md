# ADR-0002 — Idempotent Order Creation

**Status:** Accepted

## Context

Clients may retry an order creation request because of network failures or timeouts. Without idempotency, the same request could create multiple orders and reserve the customer's assets multiple times.

## Decision

Order creation supports an `Idempotency-Key`.

A unique claim is stored for each customer and idempotency key within the same transaction as the order creation and asset reservation.

- Same key + same request → return the existing result
- Same key + different request → reject the request
- Concurrent requests with the same key → only one order is created

## Rationale

This prevents duplicate orders and duplicate asset reservations while keeping the operation safe under retries and concurrent requests.