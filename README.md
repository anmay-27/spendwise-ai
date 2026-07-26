# SpendWise AI

SpendWise AI is a payment-style money manager. It simulates wallet payments, predicts an expense category using a Python ML service, learns from category corrections, tracks category budgets, warns about overspending, and produces monthly insights.

## What is included

- `backend/` — Spring Boot REST API
- `frontend/` — React + Vite interface
- `ml-service/` — FastAPI + scikit-learn categorization service
- `docker-compose.yml` — PostgreSQL and all three applications

The MVP intentionally uses simulated payments. It does not move real money and does not connect to UPI or a bank.

## Fastest way: Docker

Install Docker Desktop, open a terminal in this project folder, and run:

```bash
docker compose up --build
```

Open:

- Application: http://localhost:3000
- Spring Boot API: http://localhost:8080/api/health
- ML API docs: http://localhost:8000/docs

The seeded demo user has ID `1` and a demo wallet balance.

Stop everything with:

```bash
docker compose down
```

Delete all persisted demo data with:

```bash
docker compose down -v
```

## Run manually

### Requirements

- Java 17
- Node.js 24 LTS (Node 22 LTS also works with this project)
- Python 3.11 or newer
- IntelliJ IDEA for `backend`
- VS Code for `frontend` and `ml-service`

### 1. Start the ML service in VS Code

Open the `ml-service` folder, then use the terminal.

Windows PowerShell:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Git Bash:

```bash
python -m venv .venv
source .venv/Scripts/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Verify http://localhost:8000/health.

### 2. Start the backend in IntelliJ

1. Open IntelliJ.
2. Choose **Open** and select the `backend` folder.
3. Let IntelliJ import the Maven project.
4. Confirm that Project SDK is Java 17.
5. Run `SpendWiseApplication.java`.

By default, the backend uses an in-memory H2 database, so PostgreSQL is not required for the first run. It calls the ML service at `http://localhost:8000`.

Verify http://localhost:8080/api/health.

### 3. Start the frontend in VS Code

Open the `frontend` folder in another VS Code window:

```bash
npm install
npm run dev
```

Open http://localhost:5173.

Vite proxies `/api` requests to Spring Boot on port 8080.

## Recommended opening arrangement

```text
IntelliJ
└── backend/

VS Code window 1
└── ml-service/

VS Code window 2
└── frontend/
```

All folders are still part of the same project and communicate using HTTP APIs.

## Main flow

```text
React payment form
        ↓
Spring Boot validates wallet balance
        ↓
FastAPI predicts a category
        ↓
Spring Boot stores the transaction
        ↓
React displays the category and budget warning
```

When a user changes a category, Spring Boot sends that correction to FastAPI. The ML service remembers exact merchant corrections per user and retrains its text classifier using persisted feedback.

## API overview

- `GET /api/dashboard?userId=1`
- `POST /api/payments`
- `GET /api/transactions?userId=1`
- `PATCH /api/transactions/{id}/category`
- `GET /api/categories?userId=1`
- `POST /api/categories`
- `GET /api/budgets?userId=1`
- `POST /api/budgets`
- `GET /api/reports/monthly?userId=1`
- `POST /api/assistant/ask`

## Current MVP boundaries

Included:

- Demo wallet and simulated payments
- ML category prediction
- Personal learning from category corrections
- Custom categories
- Category budgets and warning levels
- Dashboard and monthly report
- Database-backed natural-language spending assistant

Not included yet:

- Real bank or UPI integration
- JWT authentication
- Receipt OCR
- Real LLM integration
- Push notifications
- Production payment compliance

## Next development order

1. Run all three services locally.
2. Make test payments and correct at least one category.
3. Verify that the same merchant is categorized using the correction next time.
4. Add JWT login and user registration.
5. Add receipt OCR and subscription detection.
6. Add an LLM only as an explanation layer; calculations must remain database-driven.
7. Deploy the containers and PostgreSQL.

See `DEPLOYMENT.md` for the production environment variables and deployment layout.
