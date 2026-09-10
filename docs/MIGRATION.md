# Migration and implementation decisions

## Existing installations

The repository now defaults to PostgreSQL and Flyway validation. Existing H2 memory data cannot be recovered after its process has stopped. No existing Docker volumes are deleted by the upgrade.

The Compose project is explicitly named `spendwise`. It uses a new named database volume when an earlier installation used the directory-derived project name. An old volume can therefore still exist without being attached to this stack. Do not delete it if it contains data you need.

For a populated legacy PostgreSQL database:

1. Back up the database with `pg_dump` using your existing connection settings.
2. Restore into a separate test database and inspect the schema against `V1__existing_financial_schema.sql`.
3. Check for negative wallet balances, nonpositive amounts and invalid budget limits before applying constraints.
4. Only for that verified legacy schema, start once with `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` and `SPRING_FLYWAY_BASELINE_VERSION=1`. Flyway records the pre-existing V1 schema and applies V2 onward. Never enable automatic baselining as a general default.
5. Verify row counts, ownership, totals, and backups before switching the application connection.

Legacy budgets are assigned the migration's current month. Their historical effective months cannot be inferred reliably. Legacy expense rows remain editable ledger expenses because the old schema did not distinguish simulated wallet payments from seeded records. Review that distinction before migrating real historical data.

## Service boundaries

This iteration has working deployable Gateway, finance core, notification service, and Python AI service, plus the frontend. The notification service owns its PostgreSQL database. It authenticates requests by calling the core's session-aware `/api/auth/me`; it cannot query financial tables.

Auth, transactions, budgets, analytics, goals and reporting currently remain modules inside the finance core. Extracting all six during one migration would sacrifice ownership constraints and tested transactions. No empty microservice directories were created. The next extraction should be Auth using issuer/JWKS validation plus a documented revocation contract, followed by Transaction. Replace cross-domain foreign keys with stable external IDs only when that domain actually moves.

## Delivery semantics

Financial changes and outbox insertion share one PostgreSQL transaction. The publisher waits for the Kafka acknowledgement before marking a row published. A crash between broker acknowledgement and database commit causes duplicate delivery, not silent loss. Consumers deduplicate durably in the same database transaction as their side effects.

Budget/anomaly/subscription consumers recalculate committed financial data. This tolerates out-of-order events without double increments. Budget thresholds are emitted once per budget/month/threshold, even if spending later drops and crosses the same threshold again. Existing alerts are historical facts and are not deleted after transaction edits.

Kafka uses three partitions keyed by user ID. The outbox publisher is intended to run as one active core instance in this local deployment; multiple publishers with SKIP LOCKED can reorder events. Consumer recomputation handles this, but strict per-aggregate ordering would require ordered partition dispatch or CDC before scaling writers.

## Deliberate limits

- No bank/UPI connection or transfer of real money.
- Monthly subscription heuristics require three similar payments, 25–35 days apart, with 5% amount tolerance. Weekly/yearly schedules need additional models.
- Anomalies require five prior same-category expenses and exceed both twice the mean and mean plus three population standard deviations. This is unusual-spending detection, not fraud detection.
- Savings estimates use the previous calendar month's net savings, not a forecast guarantee.
- Notification polling is every 15 seconds; WebSockets are deferred.
- PDF output uses browser Print / Save as PDF, not a server PDF renderer.
- The shared classifier uses seed examples only. Personal merchant corrections live in a user-keyed SQLite database on a persistent AI-service volume; no cross-user retraining occurs.
- SQLite suits one AI-service deployment. Move corrections to service-owned PostgreSQL before multiple AI replicas.
- An external LLM is not required or configured. Answers are deterministic calculated facts.
- Auth sessions and event deduplication use PostgreSQL rather than Redis to preserve revocation and deduplication across Redis loss.
- Analytics still aggregates bounded monthly result sets in Java. Large datasets should use SQL projections and consumer-maintained summaries.
- Local Kafka is one broker with replication factor one. Cloud readiness requires TLS, ACLs, replication, retention policy, backups, and secret management.
- Outbox, processed-event and refresh-session retention/cleanup need an operational policy before long-running production use.
- Google OAuth is preserved as an optional backend profile; provide external callback/proxy configuration and credentials before enabling it. It is not part of the default local demo.
- Kubernetes remains a future deployment option.
