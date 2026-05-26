# Render.com Deployment Guide

Complete step-by-step guide to deploy the Personal Finance Manager API to Render.com.

---

## Prerequisites

- A [Render.com](https://render.com) account (free tier works)
- Your project pushed to a GitHub or GitLab repository
- Git installed locally

---

## Step 1 — Push Code to GitHub

```bash
cd /path/to/personal-finance-manager

git init
git add .
git commit -m "Initial commit: Personal Finance Manager API"

# Create a new repo on github.com, then:
git remote add origin https://github.com/YOUR_USERNAME/personal-finance-manager.git
git branch -M main
git push -u origin main
```

Make sure `gradlew` is executable:
```bash
git update-index --chmod=+x gradlew
git commit -m "Fix gradlew permissions"
git push
```

---

## Step 2 — Deploy via Blueprint (Recommended)

The project includes `render.yaml` which automates everything.

1. Log in to [dashboard.render.com](https://dashboard.render.com)
2. Click **New +** → **Blueprint**
3. Connect your GitHub account and select your repository
4. Render detects `render.yaml` automatically
5. Click **Apply**

Render will automatically:
- Create a PostgreSQL database named `finance-db`
- Create a web service named `personal-finance-api`
- Set all environment variables from the database
- Build and deploy the app

Your API will be live at `https://personal-finance-api.onrender.com` in ~5 minutes.

---

## Step 3 — Manual Deploy (Alternative)

If you prefer to set things up manually:

### 3a. Create PostgreSQL Database

1. Click **New +** → **PostgreSQL**
2. Fill in:
   - **Name:** `finance-db`
   - **Database:** `financedb`
   - **User:** `finance_user`
   - **Region:** Choose closest to your users
   - **Plan:** Free
3. Click **Create Database**
4. Wait ~1 minute, then copy the **Internal Database URL**

### 3b. Create Web Service

1. Click **New +** → **Web Service**
2. Connect your GitHub repository
3. Fill in:

| Field | Value |
|---|---|
| **Name** | `personal-finance-api` |
| **Region** | Same as your database |
| **Branch** | `main` |
| **Runtime** | `Java` |
| **Build Command** | `./gradlew clean build -x test` |
| **Start Command** | `java -jar build/libs/app.jar` |
| **Plan** | Free |

### 3c. Set Environment Variables

On the Web Service page → **Environment** → add:

| Key | Value |
|---|---|
| `DATABASE_URL` | Internal Database URL from Step 3a (must start with `jdbc:postgresql://`) |
| `DB_DRIVER` | `org.postgresql.Driver` |
| `DB_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` |
| `DB_USERNAME` | `finance_user` |
| `DB_PASSWORD` | _(password from your Render DB page)_ |
| `PORT` | `8080` |

> **Important:** Render provides the DB URL as `postgresql://...`. You must prefix it with `jdbc:` to make it `jdbc:postgresql://...`

---

## Step 4 — Verify Deployment

Once the build completes:

### Health check
```bash
curl https://personal-finance-api.onrender.com/actuator/health
```
Expected:
```json
{"status":"UP","components":{"db":{"status":"UP"}}}
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

### Build fails: `./gradlew: Permission denied`
```bash
git update-index --chmod=+x gradlew
git commit -m "Fix gradlew permissions"
git push
```

### App crashes on startup: `DATABASE_URL not set`
- Go to Web Service → **Environment**
- Verify `DATABASE_URL` starts with `jdbc:postgresql://`
- Verify `DB_DRIVER` and `DB_DIALECT` are set

### App crashes: `Unable to acquire JDBC Connection`
- Make sure you're using the **Internal** URL (not External) for services in the same Render region
- External URL is for connecting from your local machine only

### 401 on all requests after login
- Make sure your HTTP client is sending the `JSESSIONID` cookie with each request
- Use `-c cookies.txt -b cookies.txt` with curl to persist cookies
- The session expires after 24 hours — log in again

### Free tier cold starts
- Render free tier spins down after 15 minutes of inactivity
- First request after sleep takes ~30 seconds
- Upgrade to Starter plan ($7/month) to avoid this

---

## Session Authentication Notes

This API uses **server-side session authentication**, not JWT tokens.

- After login, the server creates a session and returns a `JSESSIONID` cookie
- All subsequent requests must include this cookie
- Logout invalidates the session server-side
- Sessions expire after 24 hours of inactivity

**For testing with curl:**
```bash
# Always use -c (save) and -b (send) cookie flags
curl -c cookies.txt -b cookies.txt -X POST .../api/auth/login ...
curl -b cookies.txt .../api/transactions
```

**For testing with Postman:**
- Enable "Automatically follow redirects" and "Send cookies"
- Postman handles the `JSESSIONID` cookie automatically after login

**For frontend apps:**
```javascript
// Use credentials: 'include' to send cookies cross-origin
fetch('/api/transactions', {
  credentials: 'include'
})
```

---

## Render Free Tier Limits

| Resource | Free Limit |
|---|---|
| Web Service | 750 hours/month |
| PostgreSQL | 1 GB storage, 90 days expiry |
| Bandwidth | 100 GB/month |
| Build minutes | 500/month |

For production, upgrade to Starter plan ($7/month web + $7/month DB).

---

## Custom Domain (Optional)

1. Web Service → **Settings** → **Custom Domains**
2. Add your domain (e.g., `api.yourapp.com`)
3. Add a CNAME record in your DNS pointing to your Render URL
4. Render auto-provisions SSL

---

## Monitoring

- **Logs:** Web Service → **Logs** tab (real-time)
- **Metrics:** Web Service → **Metrics** tab
- **Health:** `GET /actuator/health` (no auth required)
- **Events:** Web Service → **Events** tab (deploys, restarts)

---

## Continuous Deployment

Render auto-deploys on every push to your connected branch.

To trigger a manual deploy:
- Web Service → **Manual Deploy** → **Deploy latest commit**

To disable auto-deploy:
- Web Service → **Settings** → **Auto-Deploy** → toggle off
