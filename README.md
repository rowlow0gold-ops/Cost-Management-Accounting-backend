# Cost Management Accounting — Backend

Spring Boot 3.2 + Java 17 + JPA + Spring Security (JWT) + Apache POI.

Built as the backend for 노아에이티에스 2026 공채 15기 portfolio
(원가/관리회계 통합 시스템).

## Run (local)

```bash
# Requires Java 17+
./gradlew bootRun
# -> http://localhost:8080
```

The local profile uses an in-memory **H2** database (PostgreSQL mode). H2 console is at `/h2`.

## Demo accounts

All passwords: `password123`

| Email | Role |
|---|---|
| admin@noaats.com   | ADMIN   |
| manager@noaats.com | MANAGER |
| user@noaats.com    | USER    |

## Endpoints

- `POST /api/auth/login` — `{ email, password }` → `{ token, role, ... }`
- `POST /api/auth/register`
- `GET  /api/cost/aggregate?yearMonth=2026-04&level=PROJECT|DEPARTMENT|EMPLOYEE|COMPANY`
- `POST /api/cost/allocate` — body `{ yearMonth, basis: HOURS|HEADCOUNT|REVENUE }`
- `POST /api/cost/transfer`
- `GET  /api/cost/variance?yearMonth=2026-04`
- `GET/POST /api/timesheets` (+ `/{id}/submit|approve|reject`)
- `GET/POST/PUT/DELETE /api/masters/{departments|employees|projects|rates|cost-items}`
- `GET  /api/export/aggregate.xlsx`, `GET /api/export/variance.xlsx`
- `GET  /api/admin/audit` (ADMIN)

Send `Authorization: Bearer <token>` on every protected request.

## Production

Set `SPRING_PROFILES_ACTIVE=prod` and:
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` (Neon Postgres)
- `JWT_SECRET` (>= 32 chars)
- `ALLOWED_ORIGINS` (e.g., `https://your-frontend.vercel.app`)
