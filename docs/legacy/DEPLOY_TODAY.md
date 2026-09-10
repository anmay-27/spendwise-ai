# SpendWise AI — finish and deploy today

This package deploys the React frontend and Spring Boot backend as **one Render web service**. That keeps login cookies, CSRF, Google OAuth, and API calls on one origin. The FastAPI ML service and PostgreSQL database are separate Render resources.

## A. Configure Google OAuth locally

1. Open Google Cloud Console and create/select a project.
2. Open **Google Auth Platform**.
3. Configure **Branding**:
   - App name: `SpendWise AI`
   - User support email: your Gmail
   - Developer contact: your Gmail
4. Under **Audience**, select **External** and keep the app in testing while developing.
5. Add your Gmail account under **Test users**.
6. Under **Clients**, create a client:
   - Application type: **Web application**
   - Name: `SpendWise Web`
7. Add the local authorized redirect URI:

```text
http://localhost:8081/login/oauth2/code/google
```

8. Save and copy the client ID and client secret.

### IntelliJ environment variables

Open **Run > Edit Configurations > SpendWiseApplication > Environment variables** and use separate rows:

```text
SPRING_PROFILES_ACTIVE=postgres,google
DB_URL=jdbc:postgresql://localhost:55432/spendwise
DB_USERNAME=spendwise
DB_PASSWORD=spendwise
ML_SERVICE_URL=http://localhost:8000
FRONTEND_URL=http://localhost:5173
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
APP_JWT_SECRET=spendwise-local-secret-that-is-definitely-longer-than-32-characters
COOKIE_SECURE=false
COOKIE_SAME_SITE=Lax
SESSION_COOKIE_SECURE=false
```

### Frontend local environment

Create `frontend/.env.local`:

```text
VITE_GOOGLE_LOGIN_ENABLED=true
```

Restart Vite after creating this file.

### Local verification

Run PostgreSQL, ML, backend, and frontend. Click **Continue with Google**. Google must return to SpendWise and the dashboard must open.

---

## B. Put the project on GitHub

From the project root:

```powershell
git init
git add .
git commit -m "Prepare SpendWise AI for deployment"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/spendwise-ai.git
git push -u origin main
```

Before pushing, confirm these are not committed:

```text
.env
frontend/.env.local
GOOGLE_CLIENT_SECRET
real database passwords
```

---

## C. Deploy using Render Blueprint

1. Sign in to Render with GitHub.
2. In Render, choose **New > Blueprint**.
3. Connect the `spendwise-ai` repository.
4. Render automatically reads the root `render.yaml`.
5. During Blueprint creation, Render asks for:
   - `GOOGLE_CLIENT_ID`
   - `GOOGLE_CLIENT_SECRET`
6. Paste the Google values and click **Deploy Blueprint**.
7. Wait for these resources:
   - `spendwise-db`
   - `spendwise-ml-service`
   - `spendwise-app`
8. Open the `spendwise-app` service after it becomes live. Its URL will look similar to:

```text
https://spendwise-app.onrender.com
```

The exact name may have a suffix if that service name is already taken.

---

## D. Add the production Google callback

After Render gives the final application URL, return to Google Cloud > Google Auth Platform > Clients > SpendWise Web.

Add this exact authorized redirect URI, replacing the hostname with your real Render hostname:

```text
https://YOUR-SPENDWISE-APP.onrender.com/login/oauth2/code/google
```

Optionally add these authorized JavaScript origins:

```text
http://localhost:5173
http://localhost:8081
https://YOUR-SPENDWISE-APP.onrender.com
```

Save, wait a minute, and test **Continue with Google** on the deployed site.

---

## E. Final deployed checklist

- `/api/health` returns `status: ok`
- Register with email/password
- Log out and log in again
- Sign in with Google
- Make a demo payment
- AI category suggestion appears
- Wallet balance decreases
- Transaction appears in history
- Restart/redeploy the app and confirm data remains
- A second user cannot see the first user's data

## Important notes

- Payments are simulated; the project does not transfer real money.
- Free Render web services may sleep after inactivity and take time on the first request.
- Free Render PostgreSQL is suitable for a demo but expires after the platform's free-database retention period.
- Do not run `docker compose down -v` locally unless you intend to erase local PostgreSQL data.
