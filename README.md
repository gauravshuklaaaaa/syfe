# Personal Finance Manager API

A production-ready REST API for managing personal finances — track income/expenses, set savings goals, and generate monthly/yearly reports. Built with Spring Boot 3, PostgreSQL/H2, and session-based authentication.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Quick Start (Local)](#quick-start-local)
- [Environment Variables](#environment-variables)
- [Running Tests](#running-tests)
- [API Reference](#api-reference)
- [Authentication Flow](#authentication-flow)
- [Default Categories](#default-categories)
- [Error Codes](#error-codes)
- [Deploy to Render.com](#deploy-to-rendercom)

---

## Features

- **Session-based Authentication** — register, login, logout with secure server-side sessions and cookies
- **Transaction Management** — full CRUD with filtering by date range, category, and type; sorted newest first
- **Category Management** — 7 built-in default categories + user-defined custom categories
- **Savings Goals** — create goals with automatic progress calculation from real transactions
- **Reports & Analytics** — monthly and yearly income/expense breakdowns by category
- **Input Validation** — Jakarta Bean Validation on all request bodies
- **Data Isolation** — every user sees only their own data, enforced at service layer
- **Swagger UI** — interactive API docs at `/swagger-ui.html`
- **Actuator Health** — `/actuator/health` for uptime monitoring

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Database | PostgreSQL 14+ (H2 for tests) |
| ORM | Spring Data JPA + Hibernate |
| Auth | Spring Security — Session-based (cookies) |
| Build | Gradle 8+ |
| Testing | JUnit 5 + MockMvc + H2 |
| Docs | Springdoc OpenAPI 2.5 (Swagger UI) |
| Deployment | Render.com |

---

## Project Structure

```
src/
├── main/java/com/finance/manager/
│   ├── FinanceManagerApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java          ← Session auth, CORS, entry points
│   │   ├── DataInitializer.java         ← Seeds default categories on startup
│   │   └── OpenApiConfig.java           ← Swagger configuration
│   ├── controller/
│   │   ├── AuthController.java          ← /api/auth/**
│   │   ├── CategoryController.java      ← /api/categories/**
│   │   ├── TransactionController.java   ← /api/transactions/**
│   │   ├── SavingsGoalController.java   ← /api/goals/**
│   │   └── ReportController.java        ← /api/reports/**
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── CategoryService.java
│   │   ├── TransactionService.java
│   │   ├── SavingsGoalService.java
│   │   └── ReportService.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── CategoryRepository.java
│   │   ├── TransactionRepository.java
│   │   └── SavingsGoalRepository.java
│   ├── model/
│   │   ├── User.java
│   │   ├── Category.java
│   │   ├── Transaction.java
│   │   ├── SavingsGoal.java
│   │   └── TransactionType.java         ← Enum: INCOME, EXPENSE
│   ├── dto/
│   │   ├── request/
│   │   │   ├── RegisterRequest.java
│   │   │   ├── LoginRequest.java
│   │   │   ├── CategoryRequest.java
│   │   │   ├── TransactionRequest.java
│   │   │   └── SavingsGoalRequest.java
│   │   └── response/
│   │       ├── CategoryResponse.java
│   │       ├── TransactionResponse.java
│   │       ├── SavingsGoalResponse.java
│   │       ├── MonthlyReportResponse.java
│   │       └── YearlyReportResponse.java
│   ├── security/
│   │   └── CustomUserDetailsService.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       ├── BadRequestException.java
│       ├── ConflictException.java
│       ├── ForbiddenException.java
│       └── ResourceNotFoundException.java
└── test/java/com/finance/manager/
    └── controller/
        ├── AuthControllerTest.java
        ├── CategoryControllerTest.java
        ├── TransactionControllerTest.java
        ├── SavingsGoalControllerTest.java
        └── ReportControllerTest.java
```

---

## Quick Start (Local)

### Prerequisites

- Java 17+
- Gradle 8+ (or use the included `./gradlew` wrapper)
- PostgreSQL 14+ (optional — H2 is used by default for local dev)

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/personal-finance-manager.git
cd personal-finance-manager
```

### 2. Run with H2 (no database setup needed)

```bash
./gradlew bootRun
```

The app starts at `http://localhost:8080` using an in-memory H2 database.

### 3. Run with PostgreSQL

```bash
# Create database
psql -U postgres -c "CREATE DATABASE financedb;"
psql -U postgres -c "CREATE USER finance_user WITH PASSWORD 'yourpassword';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE financedb TO finance_user;"

# Set environment variables
export DATABASE_URL=jdbc:postgresql://localhost:5432/financedb
export DB_DRIVER=org.postgresql.Driver
export DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect
export DB_USERNAME=finance_user
export DB_PASSWORD=yourpassword

./gradlew bootRun
```

### 4. Verify

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}

# Swagger UI:
# http://localhost:8080/swagger-ui.html
```

---

## Environment Variables

| Variable | Description | Default (local) |
|---|---|---|
| `DATABASE_URL` | JDBC connection URL | H2 in-memory |
| `DB_DRIVER` | JDBC driver class | `org.h2.Driver` |
| `DB_DIALECT` | Hibernate dialect | `H2Dialect` |
| `DB_USERNAME` | Database username | `sa` |
| `DB_PASSWORD` | Database password | _(empty)_ |
| `PORT` | Server port | `8080` |

---

## Running Tests

Tests use H2 in-memory — no PostgreSQL required.

```bash
# Run all tests
./gradlew test

# Run with output
./gradlew test --info

# Run specific test class
./gradlew test --tests "com.finance.manager.controller.AuthControllerTest"

# View HTML report
open build/reports/tests/test/index.html
```

**Test coverage includes:**
- Registration success/failure/duplicate
- Login success/wrong password
- Logout and session invalidation
- Unauthenticated access returns 401
- Default categories present on startup
- Custom category create/delete/conflict
- Default category deletion returns 403
- In-use category deletion returns 400
- Transaction CRUD with session
- Future date transaction returns 400
- Transaction update ignores date field
- Data isolation between users
- Savings goal CRUD
- Goal progress calculated from transactions
- Cross-user goal access returns 403
- Monthly and yearly reports
- Invalid month returns 400

---

## API Reference

### Base URL

- Local: `http://localhost:8080`
- Production: `https://personal-finance-api.onrender.com`

### Authentication

All protected endpoints require a valid session cookie (`JSESSIONID`) obtained from `/api/auth/login`.

---

### Auth Endpoints

#### POST /api/auth/register

```json
// Request
{
  "username": "user@example.com",
  "password": "password123",
  "fullName": "John Doe",
  "phoneNumber": "+1234567890"
}

// Response 201
{
  "message": "User registered successfully",
  "userId": 1
}
```

#### POST /api/auth/login

```json
// Request
{
  "username": "user@example.com",
  "password": "password123"
}

// Response 200 (sets JSESSIONID cookie)
{
  "message": "Login successful"
}
```

#### POST /api/auth/logout

```json
// Response 200 (invalidates session)
{
  "message": "Logout successful"
}
```

---

### Category Endpoints

#### GET /api/categories

```json
// Response 200
{
  "categories": [
    { "name": "Salary", "type": "INCOME", "isCustom": false },
    { "name": "Food", "type": "EXPENSE", "isCustom": false },
    { "name": "MyCategory", "type": "EXPENSE", "isCustom": true }
  ]
}
```

#### POST /api/categories

```json
// Request
{ "name": "Freelance", "type": "INCOME" }

// Response 201
{ "name": "Freelance", "type": "INCOME", "isCustom": true }
```

#### DELETE /api/categories/{name}

```json
// Response 200
{ "message": "Category deleted successfully" }
```

---

### Transaction Endpoints

#### POST /api/transactions

```json
// Request
{
  "amount": 50000.00,
  "date": "2024-01-15",
  "category": "Salary",
  "description": "January Salary"
}

// Response 201
{
  "id": 1,
  "amount": 50000.00,
  "date": "2024-01-15",
  "category": "Salary",
  "description": "January Salary",
  "type": "INCOME"
}
```

#### GET /api/transactions

Query params: `startDate`, `endDate` (YYYY-MM-DD), `categoryId`, `type` (INCOME/EXPENSE)

```json
// Response 200
{
  "transactions": [
    {
      "id": 1,
      "amount": 50000.00,
      "date": "2024-01-15",
      "category": "Salary",
      "description": "January Salary",
      "type": "INCOME"
    }
  ]
}
```

#### PUT /api/transactions/{id}

Date field is ignored on update. Only `amount`, `category`, `description` can be changed.

#### DELETE /api/transactions/{id}

```json
{ "message": "Transaction deleted successfully" }
```

---

### Savings Goal Endpoints

#### POST /api/goals

```json
// Request
{
  "goalName": "Emergency Fund",
  "targetAmount": 5000.00,
  "targetDate": "2026-01-01",
  "startDate": "2025-01-01"
}

// Response 201
{
  "id": 1,
  "goalName": "Emergency Fund",
  "targetAmount": 5000.00,
  "targetDate": "2026-01-01",
  "startDate": "2025-01-01",
  "currentProgress": 1000.00,
  "progressPercentage": 20.0,
  "remainingAmount": 4000.00
}
```

Progress is calculated automatically: `currentProgress = totalIncome - totalExpenses` since `startDate`.

#### GET /api/goals — list all goals
#### GET /api/goals/{id} — single goal
#### PUT /api/goals/{id} — update targetAmount, targetDate, goalName
#### DELETE /api/goals/{id}

```json
{ "message": "Goal deleted successfully" }
```

---

### Report Endpoints

#### GET /api/reports/monthly/{year}/{month}

```json
// Response 200
{
  "month": 1,
  "year": 2024,
  "totalIncome": { "Salary": 3000.00 },
  "totalExpenses": { "Food": 400.00, "Rent": 1200.00 },
  "netSavings": 1400.00
}
```

#### GET /api/reports/yearly/{year}

```json
// Response 200
{
  "year": 2024,
  "totalIncome": { "Salary": 36000.00 },
  "totalExpenses": { "Food": 4800.00 },
  "netSavings": 31200.00
}
```

---

## Default Categories

These are seeded on startup and cannot be deleted:

| Name | Type |
|---|---|
| Salary | INCOME |
| Food | EXPENSE |
| Rent | EXPENSE |
| Transportation | EXPENSE |
| Entertainment | EXPENSE |
| Healthcare | EXPENSE |
| Utilities | EXPENSE |

---

## Error Codes

| HTTP | Code | Cause |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Missing/invalid request fields |
| 400 | `BAD_REQUEST` | Future transaction date, in-use category deletion, invalid month |
| 401 | `UNAUTHORIZED` | No session, wrong credentials |
| 403 | `FORBIDDEN` | Accessing another user's resource, deleting default category |
| 404 | `NOT_FOUND` | Resource does not exist |
| 409 | `CONFLICT` | Duplicate username or category name |
| 500 | `INTERNAL_SERVER_ERROR` | Unexpected error |

---

## Deploy to Render.com

See [GUIDE.md](GUIDE.md) for the full step-by-step guide.

**Quick summary:**
1. Push to GitHub
2. Go to Render → **New** → **Blueprint**
3. Connect your repo — `render.yaml` is auto-detected
4. Click **Apply** — database and web service are provisioned automatically
5. API is live in ~5 minutes

---

## cURL Examples

```bash
BASE=http://localhost:8080

# Register
curl -c cookies.txt -X POST $BASE/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"jane@example.com","password":"secret123","fullName":"Jane Doe","phoneNumber":"+1234567890"}'

# Login (saves session cookie to cookies.txt)
curl -c cookies.txt -b cookies.txt -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"jane@example.com","password":"secret123"}'

# Add income transaction
curl -b cookies.txt -X POST $BASE/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"amount":5000,"date":"2024-01-01","category":"Salary","description":"January"}'

# Get all transactions
curl -b cookies.txt $BASE/api/transactions

# Monthly report
curl -b cookies.txt $BASE/api/reports/monthly/2024/1

# Yearly report
curl -b cookies.txt $BASE/api/reports/yearly/2024

# Create savings goal
curl -b cookies.txt -X POST $BASE/api/goals \
  -H "Content-Type: application/json" \
  -d '{"goalName":"Vacation","targetAmount":3000,"targetDate":"2026-12-31"}'

# Logout
curl -b cookies.txt -X POST $BASE/api/auth/logout
```

---

## Security Notes

- Passwords hashed with BCrypt (strength 10)
- Sessions stored server-side; client holds only a `JSESSIONID` cookie
- Session timeout: 24 hours
- CSRF disabled (stateless API clients)
- All `/api/**` routes except register/login require a valid session
- Data ownership enforced at service layer — users cannot access other users' data
