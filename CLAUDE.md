# HabitQuest: notes for Claude

Product rules live in docs/PRD.md (source of truth). How to run and deploy is in README.md.

## Local development
- Postgres runs in Docker on **5433** (`docker compose up -d`). A native Postgres already uses 5432.
- The user runs their own backend (IntelliJ) on **8080** and Vite on **5173/5174**. Never stop these.
  For testing, use backend **8081** (`SERVER_PORT=8081`) and Vite **5175** (`API_TARGET=http://localhost:8081 npx vite --port 5175`).
- Backend tests (`cd backend && ./mvnw verify`) need the Docker database running.

## Deployment & infrastructure
- **Backend:** Heroku app `habitquest-api` (Eco dyno plan $5, shared by all Eco apps on the account + heroku-postgresql
  essential-0 $5 = $10/month of the $13 GitHub Student Pack credit). Heroku account: aungpaingtun577@gmail.com.
  Eco dynos sleep after 30 min idle; the first request after that takes ~10-15 s while Spring Boot starts.
  Config var `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5` keeps room in the 20-connection DB for a second app
  sharing it later (give that app its own schema).
  Deploy from the repo root: `git subtree push --prefix backend heroku main`
  Health: https://habitquest-api-35269eb9ad8d.herokuapp.com/actuator/health
  The database login comes from `JDBC_DATABASE_*` (set by the Java buildpack); `JWT_SECRET` is a config var.
- **Frontend:** Cloudflare Worker `habitquest` (frontend/wrangler.jsonc). It serves the React build and forwards `/api/*` to
  Heroku (frontend/worker/index.ts, `API_ORIGIN` var). Deploy: `cd frontend && npm run deploy`.
  Wrangler is logged in as aungpaingtun577@gmail.com.
- **Domain:** aungpaingtun.dev + www, registered at Name.com (expires 2027-10-02, auto-renew OFF), DNS on Cloudflare
  (nameservers aspen/nile.ns.cloudflare.com). .dev is HSTS-preloaded, so it's HTTPS only.
- **CI:** .github/workflows/ci.yml runs backend tests against a Postgres 16 service on 5433, then frontend lint and build.
- **Demo account:** demo@aungpaingtun.dev / habitquest-demo (public on purpose). Refresh it on prod with
  `heroku run:detached -a habitquest-api -- java -Dspring.profiles.active=demo-seed -jar target/backend-0.0.1-SNAPSHOT.jar`

## Known quirks
- The user's office network blocks aungpaingtun.dev ("Web Page Blocked, category high-risk") and outbound port 5432.
  Test the live site on phone data. `heroku pg:psql` and attached `heroku run` time out here, so use `heroku run:detached` and
  read the output with `heroku logs --dyno run.N`.
- Heroku logs echo one-off commands with `$DATABASE_URL` expanded. This leaked the DB password once (rotated 2026-10-02).
- Claude's auto mode blocks `wrangler deploy` and `heroku run`, so the user runs those with `!`.

## Git
Commit and push after each verified step. Never add Co-Authored-By or any Claude attribution.
