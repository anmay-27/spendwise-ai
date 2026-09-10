# SpendWise AI

SpendWise AI is a personal finance application with income/expense tracking, monthly budgets, savings goals, calculated financial insights, a real Python transaction classifier, subscription detection and asynchronous in-app notifications.

Payments are simulated. The project does not connect to bank accounts or transfer money.

## Implemented features

- Login/register, BCrypt, 15-minute JWT access cookies, rotating refresh tokens and revocation.
- Authenticated ownership checks, CSRF, validation and consistent core API errors.
- Income/expense CRUD, merchant/notes search, date/category/type filtering, pagination and sorting.
- Wallet simulation with row locking and idempotency keys.
- Monthly category budgets, progress and 80/90/100% event alerts.
- Income, expenses, savings rate, category/merchant totals, daily/weekly/monthly trends and comparisons.
- Savings goals with optimistic updates, required savings and estimated completion.
- Hybrid categorization, private persistent corrections and a Java fallback/circuit breaker.
- Monthly recurring subscription detection and interpretable unusual-spending signals.
- Grounded spending chat with no external LLM requirement.
- In-app notifications, monthly reports and browser PDF printing.
- React light/dark design, Gateway, Docker, Swagger and operations dashboards.

## Architecture and service boundaries

```text
Browser → React/Nginx → Spring Cloud Gateway
                           ├─ Finance core → PostgreSQL
                           │      ├─ Redis
                           │      ├─ FastAPI AI → persistent private corrections
                           │      └─ Transactional outbox → Kafka
                           └─ Notification service → its own PostgreSQL
                                      ↑ notification-events
Prometheus scrapes the Java services → Grafana
```

Runnable components are `backend/`, `services/api-gateway/`, `services/notification-service/`, `ml-service/` and `frontend/`. Auth, Transaction, Budget, Analytics, Goal and Reporting remain domain modules within the core. This is a working incremental extraction; it does not claim all target domains are independent microservices.

See [project flows](docs/PROJECT_FLOW.md), [migration decisions and limitations](docs/MIGRATION.md), and the [interview guide](docs/INTERVIEW_GUIDE.md).

## Technology stack

Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA/JDBC, Spring Cloud Gateway 2025.0, PostgreSQL 17, Flyway, Redis 7.4, Kafka 4.0 KRaft, React 19/Vite 7, Python/FastAPI/scikit-learn, Resilience4j, Actuator, Prometheus, Grafana, OpenAPI, JUnit/Testcontainers, pytest and GitHub Actions.

## REST, Kafka, outbox and idempotency

REST handles synchronous login, transactions, dashboards, budgets, goals, reports and chat.

Financial writes and JSON outbox events commit atomically. The publisher sends `transaction-events` keyed by user ID, waits for broker acknowledgement, then marks rows published. Consumers insert a durable event ID and calculate effects in one transaction. A retry after a crash can duplicate a message but cannot duplicate its committed effects.

Supported transaction events: `TRANSACTION_CREATED`, `TRANSACTION_UPDATED`, `TRANSACTION_DELETED`. Derived notifications include `BUDGET_THRESHOLD_REACHED`, `SPENDING_ANOMALY_DETECTED`, `SUBSCRIPTION_DETECTED`, `GOAL_MILESTONE_REACHED` and `REPORT_READY`.

Events contain `eventId`, `eventType`, `transactionId`, `userId`, `timestamp`, `schemaVersion` and `payload`. JSON is serialized explicitly with Jackson; Kafka transports strings. Failed consumers retry and use dead-letter topics. Monitor and investigate dead letters before replaying them.

## Redis and database

PostgreSQL is authoritative. Financial tables have foreign keys, indexes, uniqueness/amount constraints and Flyway migrations. Notification storage uses a separate database and credentials.

Dashboard/analytics entries have 60-second TTLs and keys containing owner, month and committed database version. Merchant predictions use short-lived user-scoped keys. Rate counters use an atomic Lua increment/expiry. Sessions and processed events deliberately stay in PostgreSQL for durable revocation and atomic deduplication.

## AI

The Python service checks private merchant corrections, then rules, then TF-IDF/logistic regression. User corrections are persisted in service-owned SQLite and never retrain the shared classifier. The volume survives container recreation.

Java uses a circuit breaker and timeout with a rule fallback. Category corrections commit to the ledger immediately and reach Python through the outbox/Kafka consumer, with retries on failure. Subscription detection requires three similar monthly charges; unusual spending requires sufficient category history. Chat amounts are computed from owned database rows. No LLM key is needed.

## Security

Set random JWT, database, AI-service and Grafana secrets. The setup script generates them in ignored `.env`; committed examples contain placeholders. Demo seeding is off unless explicitly enabled.

Cookies are HttpOnly; CSRF protects mutations. Local HTTP uses `COOKIE_SECURE=false`; production requires HTTPS and `COOKIE_SECURE=true`. JWT signatures and active sessions are checked. Refresh tokens are hashed, rotated and revoked on logout/reuse. The AI API requires `X-Service-Key` and is not published on a host port in Compose.

Only frontend/Gateway and loopback development/monitoring ports are exposed. Management listeners are internal to the Docker network. Never expose PostgreSQL, Redis, Kafka or Actuator publicly using this development configuration.

## Setup

Required for the full container demo: Docker Desktop with Linux containers and Docker Compose, plus Git. For host development use JDK 21, Node 24 and Python 3.12 (the Python service can instead stay in Docker). Maven is supplied by the checked-in Wrapper.

```powershell
java --version
docker --version
docker compose version
docker info
git --version
node --version
```

From this repository directory, on a fresh checkout:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/setup-local.ps1 -Demo
docker compose up --build -d
docker compose ps
```

The setup script refuses to overwrite an existing `.env`. In this upgraded workspace it has already been generated. Read `DEMO_PASSWORD` locally from that file; log in as `demo@spendwise.local`. Do not share that file.

Without demo data, omit `-Demo` and register an account in the UI. Seeding only runs when enabled and the user table is empty.

| Component | URL |
|---|---|
| Application | http://localhost:3000 |
| Gateway / core Swagger | http://localhost:18080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:18080/v3/api-docs |
| Prometheus | http://localhost:9095 |
| Grafana | http://localhost:3001 |

Grafana username is `admin`; password is `GRAFANA_PASSWORD` in `.env`. The SpendWise Operations dashboard is provisioned automatically.

Stop without deleting data:

```powershell
docker compose down
```

Do not remove volumes unless you intentionally want to erase stored data. See [migration instructions](docs/MIGRATION.md) before pointing Flyway at an older populated database.

## Environment variables

| Variable | Purpose |
|---|---|
| POSTGRES_PASSWORD | Finance database password |
| NOTIFICATION_DB_PASSWORD | Independent notification database password |
| APP_JWT_SECRET | Random HMAC key, at least 32 bytes |
| ML_SERVICE_KEY | Shared internal AI credential, at least 32 characters |
| GRAFANA_PASSWORD | Grafana administrator password |
| COOKIE_SECURE | false only for local HTTP; true with HTTPS |
| DEMO_ENABLED / DEMO_PASSWORD | Explicit demo seeding and generated login password |
| DEMO_INITIAL_WALLET_BALANCE | Simulated starting wallet balance |
| GATEWAY_PORT | Optional host Gateway port; defaults to 18080 |
| APP_TIME_ZONE | Finance date/time zone; defaults to Asia/Kolkata in Compose |
| DB_URL / DB_USERNAME / DB_PASSWORD | Host/cloud Java database connection |
| ML_SERVICE_URL / REDIS_HOST / KAFKA_BOOTSTRAP_SERVERS | Externalized infrastructure addresses |
| FRONTEND_URL / CORS_ALLOWED_ORIGINS | Approved frontend origins |
| EVENTS_ENABLED / CACHE_ENABLED | Development switches; enabled in Compose |

Spring does not automatically read root `.env` when launched on the host. Set those variables in the terminal or IDE. Compose forwards them explicitly. Google OAuth requires the `google` profile, Google credentials, and a correctly configured external callback URL; it is optional.

## Local development and testing

```powershell
# JDK 21 selected in JAVA_HOME
.\mvnw.cmd -B -ntp verify

# Frontend
Set-Location frontend
npm.cmd ci
npm.cmd run dev
# Vite routes API requests through localhost:18080 Gateway

# Python, from ml-service
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt httpx
.\.venv\Scripts\python.exe -m pytest -q
```

Java integration tests use disposable Testcontainers PostgreSQL databases; Docker must be running. They cover ownership, validation, dates, refresh rotation, consumer deduplication and analytics. Maven can skip container tests if Docker is unavailable; inspect the test summary rather than assuming integration coverage ran.

GitHub Actions builds/tests Java and Python, builds React and Docker images, starts the stack, and runs the Playwright browser flow. With Compose running locally, run `npm run test:e2e` from `frontend/` (install Chromium first with `npx playwright install chromium`).

## API examples

Fetch `/api/auth/csrf` first and retain cookies. Mutations require the raw `XSRF-TOKEN` cookie value in `X-XSRF-TOKEN`.

- `POST /api/auth/register`, `/login`, `/refresh`, `/logout`
- `GET /api/auth/me`
- `GET /api/transactions/search?query=swiggy&type=EXPENSE&page=0&size=20`
- `POST /api/transactions`, `PUT /api/transactions/{id}`, `DELETE /api/transactions/{id}`
- `PATCH /api/transactions/{id}/category`
- `POST /api/payments` with UUID `Idempotency-Key`
- `GET /api/dashboard`, `GET /api/analytics?month=2026-08`
- `GET/POST /api/budgets`, `GET/POST/PUT/DELETE /api/goals`
- `GET /api/subscriptions`, `GET /api/insights`
- `POST /api/assistant/ask` with `{"question":"How much did I spend on food last month?"}`
- `GET /api/reports/monthly`, `POST /api/reports/generate`
- `GET /api/notifications`, `PATCH /api/notifications/{id}/read`

## Observability and deployment readiness

Prometheus/Grafana cover HTTP traffic, errors, mean latency, JVM memory, CPU and available Kafka client lag metrics. Structured ECS logs are enabled. Health/readiness checks gate Compose startup.

Cloud deployment is not performed. Replace local single-node infrastructure with appropriately secured managed services, configure TLS/origins/secrets, test backup restoration, define retention policies and benchmark capacity. The old Render blueprint predates the Gateway/event architecture and must not be treated as a verified deployment of this version.

See [verification results](docs/VALIDATION.md) for tested flows and remaining limits. Kubernetes and distributed tracing are future work, not local prerequisites.
