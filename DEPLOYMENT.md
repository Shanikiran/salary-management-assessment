# Deployment

The app is one self-contained jar: Angular UI + Spring Boot API + SQLite
file DB. It runs anywhere Java 17 runs — no database server, no Node at
runtime.

## Option A — Render (free tier, no credit card; recommended)

Needs: a free Render account (the deployer creates it — no account exists
yet, and none was invented for this assessment).

1. Push this repo to GitHub. (The seeded DB `backend/data/salary.db` is
   tracked in git on purpose — see the `!backend/data/salary.db` exception
   in `.gitignore`. If you re-seed locally, commit the DB again before
   deploying, or the Render build fails at `COPY backend/data/salary.db`.)
2. In Render: New → Web Service → pick the repo. Render detects
   `render.yaml` (Docker build, free plan, health check `/api/health`).
3. Deploy. The image ships the seeded demo DB (10,000 employees), so the
   app is usable immediately at the Render URL.
4. Verify: `<url>/api/health` → `{"status":"UP"}`, and open `<url>/` —
   the employee list loads with 10,000 total.

**Caveat — read this before demoing:** Render's free instances have an
**ephemeral filesystem**. Any employee/salary change made in the demo is
lost when the instance restarts or redeploys (the seeded DB is baked into
the image, so it comes back to the 10,000 seed). For a persistent demo,
attach a Render persistent disk (paid) and set `DB_PATH` to it, or move
the datasource to PostgreSQL (the JdbcTemplate SQL is standard — only the
`spring.datasource.url` and the `AUTOINCREMENT` line in `schema.sql` need
a glance).

## Option B — any Docker host

```bash
docker build -t salary-management .
docker run -p 8080:8080 salary-management
```

Same ephemeral-disk caveat; add `-v salary-data:/app/data` and
`-e DB_PATH=/app/data/salary.db` for persistence (the seed file must be
copied into the volume once, or re-seed: `bash scripts/seed.sh`).

## Option C — plain jar (VPS / laptop)

```bash
bash scripts/build-frontend.sh
cd backend && bash ../scripts/mvn.sh package -DskipTests
java -jar target/salary-management-1.0.0.jar   # :8080
```

The jar reads `./data/salary.db` by default (`DB_PATH` env var overrides
it). Ship `backend/data/salary.db` (seeded) next to the jar, or run
`bash scripts/seed.sh --reset` first.

## What was NOT done here

No account was created on any host and no deployment is live: every free
host above needs an account the candidate (or reviewer) creates. The
Dockerfile was **not** built locally — this sandbox has no Docker daemon
— so treat the first `docker build` as the verification step and fix any
build error it surfaces (the build itself needs only public Maven Central
and npm, no credentials).
