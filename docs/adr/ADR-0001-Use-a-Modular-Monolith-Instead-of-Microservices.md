# ADR-0001 — Use a Modular Monolith Instead of Microservices

**Status:** Accepted

## Context

The system manages orders and customer assets, with strong consistency required between order creation and asset reservation. The case does not require independent scaling, deployment, or team ownership of separate components.

Introducing microservices would add distributed transaction, communication, deployment, and operational complexity without solving a current business or technical problem.

## Decision

We use a **modular monolith with DDD boundaries**.

Order and Asset are modeled as separate domain boundaries while remaining in the same application and transactional context.

## Rationale

- Strong consistency between order and asset reservation
- Simpler transaction management
- No current need for independent scaling or deployment
- Lower operational complexity
- Clear domain boundaries allow future extraction into services if needed

Microservices can be introduced later if independent scaling, deployment, or organizational boundaries justify the additional complexity.