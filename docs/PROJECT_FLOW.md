# SpendWise project flows

## Deployment architecture

```mermaid
flowchart TD
 Browser --> Frontend["React / Nginx :3000"]
 Frontend --> Gateway["Spring Cloud Gateway :8080"]
 Gateway --> Core["Finance core: auth, transactions, budgets, analytics, goals, reports"]
 Gateway --> Notifications["Notification service"]
 Notifications -->|session validation REST| Core
 Core --> PostgreSQL["Financial PostgreSQL"]
 Core --> Redis["Redis: temporary caches and rate limits"]
 Core -->|authenticated HTTP| AI["FastAPI classifier"]
 AI --> Feedback["Service-owned SQLite corrections volume"]
 Core --> Outbox["Transactional outbox"]
 Outbox --> Kafka["Kafka KRaft"]
 Kafka -->|transaction-events| Core
 Kafka -->|notification-events| Notifications
 Notifications --> NotificationDB["Notification PostgreSQL"]
 Prometheus --> Core
 Prometheus --> Gateway
 Prometheus --> Notifications
 Grafana --> Prometheus
```

## Login, refresh and logout

```mermaid
sequenceDiagram
 participant UI as React
 participant API as Core via Gateway
 participant DB as PostgreSQL
 UI->>API: GET /api/auth/csrf
 API-->>UI: XSRF-TOKEN cookie
 UI->>API: POST /auth/login + CSRF header/cookie
 API->>DB: Load user and verify BCrypt password
 API->>DB: Save refresh-token SHA-256 hash and session
 API-->>UI: HttpOnly access and refresh cookies
 UI->>API: Authenticated request
 API->>DB: Check signed token session remains active
 API-->>UI: Owned financial data
 UI->>API: POST /auth/refresh + CSRF
 API->>DB: Lock old session, revoke, issue replacement
 API-->>UI: Rotated cookies
 UI->>API: POST /auth/logout
 API->>DB: Revoke refresh session
 API-->>UI: Clear cookies
```

Tokens default to 15-minute access and 14-day refresh lifetime. Reuse of a revoked refresh token revokes all sessions for that account. Redis is not the authority for revocation.

## Transaction creation and Kafka

```mermaid
sequenceDiagram
 participant UI as React
 participant API as Transaction module
 participant DB as PostgreSQL
 participant Publisher as Outbox publisher
 participant Kafka
 participant Consumer as Finance consumer
 UI->>API: POST /api/transactions
 API->>API: Validate DTO and authenticated ownership
 API->>DB: BEGIN
 API->>DB: Insert transaction
 API->>DB: Insert JSON outbox event, increment cache version
 API->>DB: COMMIT
 API-->>UI: 201 Created
 Publisher->>DB: Lock unpublished outbox batch
 Publisher->>Kafka: Publish keyed by user ID
 Kafka-->>Publisher: Broker acknowledgement
 Publisher->>DB: Mark published and commit
 Kafka->>Consumer: At-least-once event
 Consumer->>DB: Deduplicate and recompute within transaction
```

Wallet simulation additionally locks the wallet row and stores the response under a client-generated idempotency key. A duplicate key with the same payload returns the original response; a different payload returns 409.

## Budget and analytics updates

```mermaid
flowchart LR
 Event["Transaction created / updated / deleted"] --> Dedup["Insert processed event ID"]
 Dedup --> Recalculate["Read current committed financial rows"]
 Recalculate --> Budget["Recalculate monthly category spending"]
 Budget --> Threshold["New 80 / 90 / 100 threshold?"]
 Threshold --> Outbox["Insert notification outbox event"]
 Recalculate --> Anomaly["Check historical category deviation"]
 Anomaly --> Outbox
 Recalculate --> Subscription["Check recurring merchant pattern"]
 Subscription --> Outbox
 Mutation["Committed financial mutation"] --> Version["Increment user data version"]
 Version --> Cache["Next analytics/dashboard request uses new cache key"]
```

Budget usage is calculated, not a mutable counter. Redis entries expire after 60 seconds. Cache keys include user ID, period and a database version, so committed transaction changes cannot continue serving an earlier version.

## AI categorization and anomalies

```mermaid
flowchart TD
 Payment["Simulated payment"] --> Cache["User-scoped Redis merchant cache"]
 Cache -->|miss| Python["Authenticated FastAPI request"]
 Python --> Personal["Private merchant correction"]
 Personal -->|no correction| Rules["Merchant keyword rules"]
 Rules -->|unknown| Classifier["TF-IDF + logistic regression"]
 Classifier --> Result["Category and confidence"]
 Python --> Result
 Result --> Payment
 Failure["Timeout / open circuit"] --> Fallback["Java merchant-rule fallback"]
 Fallback --> Payment
 History["At least five earlier category expenses"] --> Stats["Mean and population standard deviation"]
 Stats --> Check["Amount > twice mean AND mean + 3 standard deviations"]
 Check --> Signal["Unusual-spending event"]
```

Corrections are stored by user and normalized merchant. They never retrain the shared classifier.

## Notifications and reports

```mermaid
sequenceDiagram
 participant Finance as Finance consumer / report API
 participant Outbox
 participant Kafka
 participant Service as Notification service
 participant DB as Notification database
 participant UI
 Finance->>Outbox: Budget / anomaly / subscription / goal / report event
 Outbox->>Kafka: notification-events
 Kafka->>Service: JSON event with UUID
 Service->>DB: INSERT ON CONFLICT(event_id) DO NOTHING
 UI->>Service: GET notifications via Gateway
 Service->>Finance: Validate user session
 Service->>DB: Query only that user's notifications
 Service-->>UI: Notification list
```

Notifications use 15-second polling. Reports use calculated data and support browser Print / Save as PDF. No external messages are sent.

