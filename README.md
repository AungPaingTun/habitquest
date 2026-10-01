# HabitQuest

Build good habits, earn points, and spend them on rewards you set yourself.

📄 Product requirements: [docs/PRD.md](docs/PRD.md)

**Stack:** Java 21 · Spring Boot 4 · PostgreSQL 16 · React + TypeScript (Vite) · Tailwind CSS

## Run locally

Requires Docker Desktop, Java 21 and Node 20+.

```bash
# 1. Database (Postgres in Docker, on localhost:5433)
docker compose up -d

# 2. Backend → http://localhost:8080
cd backend && ./mvnw spring-boot:run

# 3. Frontend → http://localhost:5173
cd frontend && npm install && npm run dev
```

Health check: `curl localhost:8080/actuator/health`

## Project layout

```
backend/    Spring Boot API (Flyway migrations in src/main/resources/db/migration)
frontend/   React app (calls the API through the /api proxy)
docker-compose.yml   local PostgreSQL
```
