# Local verification

Verified on Windows with Docker Desktop Linux containers on 10 September 2026.

## Builds and tests

- Java 21: `mvnw.cmd -B -ntp verify` passed across the finance core, API Gateway and notification service.
- Finance integration tests: 9 passed, 0 failed, 0 skipped, using a disposable PostgreSQL 17 Testcontainers database.
- Notification regression test: 1 passed. It verifies authenticated ownership and CSRF validation with multiple Cookie headers, and rejection when the CSRF header is absent.
- Python: 4 pytest tests passed, covering prediction confidence, private correction persistence/isolation and service authentication.
- React: Vite production build passed in the frontend Docker image.
- All five application images built successfully. PostgreSQL migrations applied during startup.
- Prometheus reported `up` for finance core, Gateway and notifications. Grafana's database health endpoint returned `ok`.

Integration coverage includes transaction ownership, valid amounts, month boundaries, refresh rotation/replay, expired access-cookie CSRF bootstrap, outbox creation, consumer deduplication, idempotent simulated payments, income, subscriptions and unusual spending.

## Browser checks

Final result: **2 passed, 0 failed, 0 skipped** in 33.9 seconds. The live notification mutation passed after the stale-token fix. [Captured seeded dashboard](screenshots/dashboard.png).

Run with the Compose stack running:

```powershell
Set-Location frontend
npm.cmd ci
npx.cmd playwright install chromium
npm.cmd run test:e2e
```

The first flow registers a new account, creates an expense, checks the calculated chat answer, opens finance screens, generates a report through Kafka, receives its notification and marks it read. The second signs into the opt-in seeded account, checks populated dashboard/subscription/insight screens and captures `frontend/test-results/demo-dashboard.png`. The seeded test is skipped when demo configuration is absent or disabled.

GitHub Actions includes the same browser flow after starting Compose. The workflow itself has not been executed on GitHub from this workspace.

## Bugs caught during verification

- Fixed a Windows conflict on external Gateway port 8080 by using configurable port 18080.
- Fixed frontend health probing on IPv6 localhost when Nginx listens on IPv4.
- Fixed stale CSRF headers after Spring rotates the browser cookie; each mutation reads the current cookie.
- Preserved all Cookie headers during notification authentication and parsed cookies for CSRF checking.
- Reset data-page state when switching between subscriptions, insights and notifications.
- Allowed CSRF initialization when an expired access cookie is present, so refresh can work after a reload.

## Limits of this verification

Google OAuth, cloud deployment, load/capacity testing, multi-broker failover, backup restoration and production TLS have not been verified. There is no external LLM, bank integration or real payment transfer. See [migration and operating limits](MIGRATION.md).

Local generated secrets remain in ignored `.env`. Test databases are disposable and distinct from the persisted demo database. Existing legacy volumes were not deleted.
