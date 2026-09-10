# Deployment readiness

The supported verified target is the local Docker Compose stack. No cloud deployment has been performed.

Deploy the frontend, Gateway, finance core, notification service and Python AI service as separate applications. Financial and notification databases require independent connection credentials. The old Render files are archived in `docs/legacy/`; they are not an active blueprint for this architecture.

Before a public deployment:

- Use HTTPS, `COOKIE_SECURE=true`, the actual frontend origin and restricted CORS.
- Store database passwords, JWT signing material, AI-service keys and Grafana credentials in the hosting platform's secret manager.
- Disable demo seeding and provision accounts through registration.
- Keep Redis, Kafka, databases and management listeners private; use TLS and authentication for managed infrastructure.
- Configure Kafka replication, ACLs, retention and dead-letter monitoring.
- Back up PostgreSQL and the AI corrections store and verify restoration.
- Define cleanup policies for outbox rows, processed events, sessions and notifications.
- Set CPU/memory budgets, benchmark requests and monitor alerts.
- Before multiple AI replicas, move its private SQLite corrections into a service-owned database.
- Replace the notification service's core-session lookup with an explicit distributed identity/revocation contract before fully extracting Auth.
- Configure the external Google callback URL and trusted reverse-proxy handling separately if OAuth is enabled.

Use `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `KAFKA_BOOTSTRAP_SERVERS`, `ML_SERVICE_URL`, `ML_SERVICE_KEY`, `APP_JWT_SECRET`, `CORE_URL` and `NOTIFICATION_URL` for platform-specific addresses and credentials. The local financial timezone defaults to Asia/Kolkata via `APP_TIME_ZONE`; use a consistent financial timezone across core replicas.

See [migration decisions](docs/MIGRATION.md) before attaching an existing financial database. Kubernetes and distributed tracing remain future improvements.
