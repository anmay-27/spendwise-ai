# Start here

## First opening

After extracting the project, you will see:

```text
spendwise-ai/
├── backend/       Open in IntelliJ IDEA
├── frontend/      Open in VS Code
├── ml-service/    Open in VS Code
├── docker-compose.yml
└── README.md
```

Do not move these three folders outside the main `spendwise-ai` folder.

## Run without Docker first

Use this method for the first run so you can see how the services connect.

### Terminal 1 — ML service

Open `ml-service` in VS Code.

Git Bash:

```bash
python -m venv .venv
source .venv/Scripts/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Expected test URL:

```text
http://localhost:8000/health
```

### IntelliJ — Spring Boot backend

1. Open the `backend` folder in IntelliJ.
2. Select Java 17 as the Project SDK.
3. Wait for Maven dependencies to finish loading.
4. Run `src/main/java/com/anmay/spendwise/SpendWiseApplication.java`.

Expected test URL:

```text
http://localhost:8080/api/health
```

The first local run uses H2 automatically, so you do not have to create PostgreSQL yet.

### Terminal 2 — React frontend

Open `frontend` in another VS Code window.

```bash
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

## First test inside the app

1. Open **Pay**.
2. Enter merchant `PVR Cinemas`.
3. Enter amount `650`.
4. Enter description `Movie tickets`.
5. Make the demo payment.
6. Confirm that the AI category is **Entertainment**.
7. Open **Transactions** and change a merchant category.
8. Pay the same merchant again to test personalized merchant memory.

## Later: run everything with Docker

After the manual setup works:

```bash
docker compose up --build
```

Then open:

```text
http://localhost:3000
```

## Important

This version simulates payments. It does not transfer real money and should not be connected to real UPI credentials.
