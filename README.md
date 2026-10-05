# HabitQuest

Build good habits, earn points, and spend them on rewards you set yourself.

**Live demo: https://aungpaingtun.dev**
Log in with `demo@aungpaingtun.dev` / `habitquest-demo` to see an account with two months of history, or register your own.

![CI](https://github.com/AungPaingTun/habitquest/actions/workflows/ci.yml/badge.svg)

## What it does

> **Do a habit → earn points → spend points on a reward you chose yourself.**

- **Habits:** daily, or *N* times a week or month, each worth the points you choose. Check in once a day, today only.
- **Streaks:** consecutive days (or weeks or months) earn a flat bonus: +2 from 7 days, +4 from 30. A flame next to each habit heats up as the streak grows.
- **Points and levels:** spendable balance plus lifetime XP. Spending never lowers your level. Level *n* needs 50 × n² XP.
- **Prize shop:** create rewards with a fixed cost and redeem them as often as you can afford. Each prize shows how many times you've redeemed it.
- **Stats:** weekly, monthly and yearly charts, per-habit completion, and a year heatmap.

The full rules, and the reasons behind them, are in the [product requirements](docs/PRD.md).

## Tech stack

| | |
|---|---|
| **Backend** | Java 21 · Spring Boot 4 · Spring Security (JWT) · Spring Data JPA · Flyway · PostgreSQL 16 |
| **Frontend** | React · TypeScript · Vite · Tailwind CSS · TanStack Query · Recharts |
| **Testing** | JUnit 5 · Spring Boot integration tests against a real Postgres (184 tests) |
| **CI** | GitHub Actions: backend tests, frontend lint and build on every push |
| **Hosting** | Cloudflare Workers (frontend and `/api` proxy) · Heroku (API and Postgres) · domain on Cloudflare DNS |

## How it fits together

```
Browser ──► aungpaingtun.dev (Cloudflare Worker)
               ├─ /*      → React app (static files)
               └─ /api/*  → forwarded to Heroku ──► Spring Boot API ──► PostgreSQL
```

The browser only ever talks to one origin, so there is no CORS setup.

Some design choices worth noting:

- **Points ledger:** every change (earn, bonus, redeem, undo) is a row, and the balance is their sum. History can't drift out of sync with the balance.
- **Time zones:** "today" is worked out in each user's own time zone, not the server's.
- **Safe redeeming:** the user row is locked during a redeem, so a double-click can't spend the same points twice.
- **Locked rules:** a habit's frequency and target lock after the first check-in, and a prize's cost never changes. Streaks and goals can't be gamed by editing them.

## Run locally

Requires Docker Desktop, Java 21 and Node 20+.

```bash
# 1. Database (Postgres in Docker, on localhost:5433)
docker compose up -d

# 2. Backend → http://localhost:8080
cd backend && ./mvnw spring-boot:run

# 3. Frontend → http://localhost:5173 (proxies /api to the backend)
cd frontend && npm install && npm run dev
```

Health check: `curl localhost:8080/actuator/health`. Run the backend tests with `cd backend && ./mvnw verify` (needs the Docker database running).

### Demo data

The demo account is created by a one-off seeder. It replays about 60 days of check-ins and redemptions through the real services on a simulated clock, so streaks, bonuses and levels follow the same rules as real use. It is safe to re-run, because it only deletes and recreates the demo user.

```bash
cd backend && ./mvnw -DskipTests package
java -Dspring.profiles.active=demo-seed -jar target/backend-*.jar
```

## Deploy

```bash
# Backend → Heroku (from the repo root)
git subtree push --prefix backend heroku main

# Refresh the demo account on Heroku
heroku run:detached -a habitquest-api -- java -Dspring.profiles.active=demo-seed -jar target/backend-0.0.1-SNAPSHOT.jar

# Frontend → Cloudflare
cd frontend && npm run deploy
```

On Heroku, the database login comes from the Postgres add-on, and `JWT_SECRET` is set as a config var.

## Project layout

```
backend/     Spring Boot API (Flyway migrations in src/main/resources/db/migration)
frontend/    React app, plus worker/ (Cloudflare Worker that forwards /api) and wrangler.jsonc
docs/PRD.md  Product rules and decisions log
docker-compose.yml   local PostgreSQL
```
