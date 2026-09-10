# Start SpendWise AI

Open a terminal in the directory containing `docker-compose.yml`.

1. Start Docker Desktop and verify `docker info`.
2. On a fresh checkout only, run `powershell -ExecutionPolicy Bypass -File scripts/setup-local.ps1 -Demo`. The script creates random credentials in ignored `.env` and will not overwrite an existing file.
3. Run `docker compose up --build -d`.
4. Check `docker compose ps`; startup waits for service health.
5. Open http://localhost:3000. Demo email is `demo@spendwise.local`; read `DEMO_PASSWORD` locally from `.env`.

Gateway/Swagger uses http://localhost:18080/swagger-ui/index.html. Set `GATEWAY_PORT` in `.env` if that host port is occupied. The browser frontend uses internal Gateway routing and needs no rebuild for a Gateway host-port change.

Grafana is http://localhost:3001 (`admin`, password from `GRAFANA_PASSWORD`). Prometheus is http://localhost:9095.

For host Java development, select Java 21 and run `.\mvnw.cmd verify`. A portable JDK was downloaded into ignored `.tools/jdk21` in this workspace; it does not change system Java. Fresh checkouts should install JDK 21 normally.

Full setup, architecture, tests and limitations: [README](README.md). Study guide: [Interview guide](docs/INTERVIEW_GUIDE.md).
