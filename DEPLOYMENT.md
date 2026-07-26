# Deployment plan

The repository is container-ready. Deploy only after all three services work locally.

## Production services

1. PostgreSQL database
2. Python ML service built from `ml-service/Dockerfile`
3. Spring Boot service built from `backend/Dockerfile`
4. React frontend built from `frontend/Dockerfile`

## Required backend environment variables

```text
SPRING_PROFILES_ACTIVE=postgres
DB_URL=jdbc:postgresql://HOST:PORT/DATABASE
DB_USERNAME=YOUR_USERNAME
DB_PASSWORD=YOUR_PASSWORD
ML_SERVICE_URL=https://YOUR-ML-SERVICE
PORT=8080
```

## ML service environment variables

```text
FEEDBACK_FILE=/app/data/feedback.jsonl
PORT=8000
```

Use a persistent disk or managed storage for the feedback file. Without it, learned corrections are lost when the container is replaced.

## Frontend routing

The included Nginx configuration proxies `/api` to a Docker service named `backend`. For separate cloud services, either:

- configure the hosting platform to proxy `/api` to the backend, or
- build the frontend with an external `VITE_API_URL` and update `src/api.js` accordingly.

## Before a public deployment

- Add JWT authentication and authorization.
- Restrict CORS to the deployed frontend domain.
- Use HTTPS everywhere.
- Store secrets in the platform's secret manager.
- Replace demo seeding with migrations.
- Add rate limiting and request logging.
- Do not connect real payments until security and regulatory requirements are addressed.
