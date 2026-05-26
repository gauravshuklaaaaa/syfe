# Render.com Deployment Guide (Free Tier)

Step-by-step manual setup — no Blueprint required, works on the free plan.

---

## Prerequisites

- A [Render.com](https://render.com) account (free tier)
- Code pushed to GitHub (already done)

---

## Step 1 — Create a PostgreSQL Database

1. Log in to [dashboard.render.com](https://dashboard.render.com)
2. Click **New +** → **PostgreSQL**
3. Fill in:
   - **Name:** `finance-db`
   - **Database:** `financedb`
   - **User:** `finance_user`
   - **Region:** Choose closest to you
   - **Plan:** Free
4. Click **Create Database**
5. Wait ~1 minute, then on the database page copy:
   - **Internal Database URL** (starts with `postgresql://...`)
   - **Username** and **Password**

---

## Step 2 — Create a Web Service

1. Click **New +** → **Web Service**
2. Connect your GitHub account and select the `syfe` repository
3. Fill in:

| Field | Value |
|---|---|
| **Name** | `personal-finance-api` |
| **Region** | Same as your database |
| **Branch** | `main` |
| **Runtime** | `Docker` |
| **Plan** | Free |

4. Leave build/start commands blank — the `Dockerfile` handles everything.

---

## Step 3 — Set Environment Variables

On the Web Service page → **Environment** → add these variables:

| Key | Value |
|---|---|
| `DATABASE_URL` | Internal Database URL from Step 1 (the `postgresql://...` string) |
| `DB_DRIVER` | `org.postgresql.Driver` |
| `DB_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` |
| `DB_USERNAME` | `finance_user` |
| `DB_PASSWORD` | password from your Render DB page |

> The app automatically rewrites `postgresql://` to `jdbc:postgresql://` at startup — paste the URL exactly as Render gives it.

5. Click **Save Changes** then **Deploy**.

---

## Step 4 — Verify Deployment

Once the build completes (~5 minutes):

### Health check
```bash
curl https://personal-finance-api.onrender.com/actuator/health
```
Expected:
```json
{"status":"UP"}
```

### Swagger UI
Open: `https://personal-finance-api.onrender.com/swagger-ui.html`

### Test the full flow
```bash
BASE=https://personal-finance-api.onrender.com

# Register
curl -c cookies.txt -X POST $BASE/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test@example.com","password":"password123","fullName":"Test User","phoneNumber":"+1234567890"}'

# Login (saves session cookie)
curl -c cookies.txt -b cookies.txt -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test@example.com","password":"password123"}'

# Get categories (should include 7 defaults)
curl -b cookies.txt $BASE/api/categories

# Add a transaction
curl -b cookies.txt -X POST $BASE/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"amount":5000,"date":"2024-01-15","category":"Salary","description":"Test income"}'

# Monthly report
curl -b cookies.txt "$BASE/api/reports/monthly/2024/1"

# Logout
curl -b cookies.txt -X POST $BASE/api/auth/logout
```

---

## Troubleshooting

### Build fails: permission denied on gradlew
```bash
git update-index --chmod=+x gradlew
git commit -m "Fix gradlew permissions"
git push
```

### App crashes: `DATABASE_URL not set`
- Go to Web Service → **Environment**
- Make sure `DATABASE_URL` is set to the Internal Database URL

### App crashes: `Unable to acquire JDBC Connection`
- Use the **Internal** URL (not External) — both services must be in the same Render region

### 401 on all requests after login
- Make sure your HTTP client sends the `JSESSIONID` cookie with each request
- Use `-c cookies.txt -b cookies.txt` with curl
- Sessions expire after 24 hours — log in again

### Free tier cold starts
- Render free tier spins down after 15 minutes of inactivity
- First request after sleep takes ~30 seconds

---

## Session Authentication Notes

This API uses **server-side session authentication**, not JWT tokens.

- After login, the server returns a `JSESSIONID` cookie
- All subsequent requests must include this cookie
- Logout invalidates the session server-side

**curl:** always use `-c cookies.txt -b cookies.txt`

**Postman:** enable "Send cookies" — it handles `JSESSIONID` automatically after login

**Frontend:**
```javascript
fetch('/api/transactions', { credentials: 'include' })
```

---

## Render Free Tier Limits

| Resource | Free Limit |
|---|---|
| Web Service | 750 hours/month |
| PostgreSQL | 1 GB storage, 90 days expiry |
| Bandwidth | 100 GB/month |

---

## Continuous Deployment

Render auto-deploys on every push to `main`. To trigger a manual deploy:
- Web Service → **Manual Deploy** → **Deploy latest commit**
