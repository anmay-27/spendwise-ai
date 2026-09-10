# SpendWise AI interview guide

## What the project does

SpendWise records personal income and expenses, tracks monthly category budgets and savings goals, and helps users understand spending. It categorizes simulated payments with a real text classifier, detects recurring monthly charges and unusual expenses, and creates in-app alerts.

The wallet is a simulation. No money moves through banks or UPI.

## Explain the architecture

React calls Spring Cloud Gateway. The Gateway routes finance requests to a Spring Boot core and notification requests to an independently deployed notification service. The Python AI service is called only by the backend using a service credential.

The core currently owns authentication, transactions, budgets, analytics, goals and reporting. Notification data belongs to a separate PostgreSQL database. This is an incremental service extraction, not nine independent finance services.

## Why each technology exists

| Technology | Concrete purpose |
|---|---|
| Java 21 / Spring Boot | Typed domain logic, HTTP APIs, transactions, security and application lifecycle |
| Spring Data JPA | Existing financial entities, relationships, transaction CRUD |
| JDBC | Explicit outbox, session, deduplication and analytics queries |
| PostgreSQL | Durable financial data and ACID atomicity |
| Flyway | Versioned, reviewed schema changes instead of implicit production schema updates |
| Spring Security | Authentication, ownership checks, CSRF and role restrictions |
| BCrypt | Slow, salted password hashing |
| JWT | Signed short-lived access credentials |
| Refresh tokens | Renew access without keeping a long-lived access token |
| Redis | Short-lived cached results and atomic request rate counters |
| Kafka | Asynchronous financial signals and notifications |
| Transactional outbox | Avoid the database-commit / Kafka-publish gap |
| Spring Cloud Gateway | A stable frontend entry point and routing boundary |
| Python / scikit-learn | TF-IDF text features and logistic regression classification |
| Resilience4j | Circuit-break an unavailable classifier instead of waiting on every call |
| Docker Compose | Reproduce the useful local multi-process system |
| Prometheus / Grafana | Inspect request rates, errors, latency, memory, CPU and consumer metrics |
| GitHub Actions | Repeatable Java tests, Python tests, frontend builds and container builds |

## Core concepts in plain language

**Authentication:** Which account made the request?

**Authorization:** Is that account allowed to access this particular transaction? Merely checking that someone is logged in is insufficient. The API derives user identity from authentication and checks resource ownership.

**Transaction:** A group of database changes that either all commit or all roll back. Wallet balance, payment record, idempotency response and event record must not partially succeed.

**Pessimistic locking:** A payment locks its wallet row so two concurrent debits cannot read the same balance and overwrite each other.

**Optimistic locking:** A version field detects conflicting transaction/goal updates rather than silently overwriting a newer change.

**Idempotency:** Retrying the same operation does not repeat its side effect. Payments use a request UUID. Kafka consumers use an event UUID plus a unique database constraint.

**At-least-once delivery:** A Kafka message can arrive more than once. The application deliberately handles duplicates; it does not claim end-to-end exactly-once delivery.

**Cache invalidation:** A transaction increments the owner's data version in PostgreSQL. Dashboard and analytics caches include that version in the key. Old entries expire naturally and are not reused for the next committed version.

**Service-owned data:** Notification code does not join the core's financial tables. It stores events in its own database and validates the caller through an API.

## Common questions and answers

**Why not use Kafka for login?**  
Login needs an immediate success/failure response. REST fits synchronous interactions. Kafka is useful after a financial transaction commits, when budgets and notifications can update asynchronously.

**What if PostgreSQL commits but Kafka is unavailable?**  
The outbox record remains unpublished. The publisher retries later. The financial operation can remain successful without losing its event.

**What if the publisher crashes after Kafka accepts the event?**  
The outbox might publish the event again. The consumer's unique event ID prevents duplicate effects.

**Why database deduplication instead of Redis?**  
The event marker and financial side effects can commit atomically in one database transaction. Redis eviction or restart cannot erase correctness history.

**Is JWT authentication fully stateless here?**  
No. The signature carries identity, but active-session validation supports immediate revocation. That database lookup is a deliberate security tradeoff.

**How are refresh tokens protected?**  
Only SHA-256 hashes are stored, cookies are HttpOnly, refresh requests use CSRF protection, and rotation revokes the previous session. Reuse revokes the account's sessions.

**Why CSRF if you use JWT?**  
Browsers automatically attach authentication cookies. CSRF protection is necessary for cookie-authenticated mutation requests.

**Why not use an LLM for every transaction?**  
Known merchants and private corrections are cheap and predictable. TF-IDF/logistic regression classifies unknown text without an API key. Calculations and chat facts come from the backend.

**How do you know classifier confidence is accurate?**  
The current probability is a model score, not a calibrated accuracy guarantee. A labelled evaluation set and calibration are future improvements. Rules also use heuristic confidence values.

**Is unusual spending fraud detection?**  
No. It flags large deviations from historical category amounts for user review. Small samples and changing habits can create false positives.

**How are subscriptions detected?**  
At least three similarly priced payments to a normalized merchant with roughly monthly gaps. This identifies patterns; it does not prove a contractual subscription.

**Why not extract every microservice immediately?**  
The domains initially share ACID transactions and ownership relationships. This version extracts real Gateway, notification and AI boundaries and documents the remaining extraction work. Empty services would add deployment costs without independent responsibility.

**What happens when Redis fails?**  
Dashboard/analytics recompute from PostgreSQL. Rate-limited mutations fail closed rather than silently bypassing protection. Financial storage and session revocation remain durable.

**What would you improve for production?**  
Service-to-service identity, asymmetric JWT/JWKS validation, TLS and Kafka ACLs, distributed tracing, database backups, restore drills, retention jobs, richer classifier evaluation, paged SQL projections and load testing. The local Kafka broker is not highly available.

## Suggested demonstration

1. Log in with the explicitly enabled demo account.
2. Explain income, expenses, savings, trends and calculated insights.
3. Search and filter transactions; create income and an expense.
4. Show a category correction and make a simulated payment.
5. Show the monthly budget and an asynchronous threshold alert.
6. Open subscriptions and explain the three-payment rule.
7. Ask “How much did I spend on food last month?”
8. Create a savings goal and update progress.
9. Generate a monthly report and show its notification.
10. Open Swagger, then Grafana.
11. Explain a test that prevents one user accessing another user's data.

## A 60-second explanation

“SpendWise AI is a personal finance platform built with Java 21, Spring Boot, React and PostgreSQL. Users record income and expenses, manage monthly budgets and savings goals, and get calculated financial insights. A Python service combines personal merchant corrections, rules and a TF-IDF logistic-regression classifier, so the demo works without an LLM API key. Spring Security uses BCrypt, short-lived JWT cookies and rotating refresh tokens, with ownership checks on financial resources. When a transaction commits, an outbox record commits with it. A publisher sends that event to Kafka, and idempotent consumers calculate alerts and persist notifications in a separate service-owned database. Redis caches dashboard results and limits sensitive requests. Gateway provides one frontend entry point, Docker Compose runs the stack, and Prometheus and Grafana expose operational metrics. I migrated incrementally and kept the remaining finance domains together where shared transactions still make sense.”

