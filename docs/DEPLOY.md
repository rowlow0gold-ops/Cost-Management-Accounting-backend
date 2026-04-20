# Deploy — Free Stack

Vercel (frontend) + Render (backend, Docker) + Neon (Postgres).
All three services have permanent free tiers (as of 2026-04).

## 1) Postgres (Neon)

1. Create an account at https://neon.tech
2. New project → name `cost-mgmt-db` → region nearest you.
3. Copy the connection string. Example:
   `postgresql://noaats:pwd@ep-xxx.ap-northeast-2.aws.neon.tech/neondb`
4. Extract host/user/password — you'll need them as separate env vars.

## 2) Backend (Render)

1. Push `Cost-Management-Accounting-backend` to GitHub.
2. https://dashboard.render.com → New → **Blueprint** → point at the repo.
   (Render will read `render.yaml` automatically.)
3. Set the `sync:false` env vars in the dashboard:
   - `DATABASE_URL` → the Neon JDBC URL:
     `jdbc:postgresql://ep-xxx.ap-northeast-2.aws.neon.tech/neondb?sslmode=require`
   - `DATABASE_USERNAME` → Neon user
   - `DATABASE_PASSWORD` → Neon password
   - `ALLOWED_ORIGINS` → `https://your-frontend.vercel.app`
4. Deploy. Wait for build (~6–8 min first time).
5. Note the service URL (e.g. `https://cost-management-backend.onrender.com`).

## 3) Frontend (Vercel)

1. Push `Cost-Management-Accounting-frontend` to GitHub.
2. https://vercel.com/new → import the repo.
3. Env var:
   - `NEXT_PUBLIC_API_BASE` = backend URL from step 2.
4. Deploy.

## 4) Verify

- Visit the Vercel URL → login with `admin@noaats.com / password123`.
- Dashboard loads data from backend.
- Note: Render free tier cold-starts after 15 min idle; first request may take ~30s.

## Common pitfalls

- **CORS blocked**: update `ALLOWED_ORIGINS` on Render with the exact Vercel URL (no trailing slash).
- **401 on every request**: frontend can't reach backend → check `NEXT_PUBLIC_API_BASE`.
- **H2 still active in prod**: ensure `SPRING_PROFILES_ACTIVE=prod` is set.
- **Seed data missing in prod**: `data-h2.sql` is not loaded under the prod profile. Insert prod seeds via a migration tool or a one-off `/api/auth/register` call.
