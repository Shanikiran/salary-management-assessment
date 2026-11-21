# ACME Salary Management — Incubyte take-home assessment

Web software replacing ACME org's Excel-based salary handling, so one HR
Manager can manage pay for 10,000 employees across multiple countries and
answer "how do we pay people?" questions quickly and reliably.

Stack: **Java 17 + Spring Boot 3.3** (REST API, SQLite) + **Angular 20**
(frontend). See `requirements.md` for the full brief, scope and deliberate
exclusions (no payroll, no currency conversion, no multi-role auth).

## Prerequisites

- Java 17, Maven 3.9 (or use `scripts/mvn.sh`, which sets the sandbox proxy
  and pins IPv4 — see below)
- Node.js v24 + npm 10 (Angular 20)
- Python 3 (seed script only)

> **Sandbox proxy note:** this environment reaches the internet through an
> egress proxy whose credentials rotate per shell (`http_proxy` /
> `https_proxy` env vars). `npm` picks them up from the environment as-is —
> no extra npm proxy config was needed. Maven needs `scripts/mvn.sh`,
> which regenerates `~/.m2/settings.xml` from the *current* env values and
> forces IPv4 (`-Djava.net.preferIPv4Stack=true`).

## Seed the database (10,000 employees)

```bash
bash scripts/seed.sh --reset        # writes backend/data/salary.db
```

## Run the backend (API on :8080 by default)

```bash
cd backend
bash ../scripts/mvn.sh spring-boot:run
# or: bash ../scripts/mvn.sh -q spring-boot:run -Dspring-boot.run.arguments=--server.port=18080
```

Health check: `curl http://localhost:8080/api/health` → `{"status":"UP"}`.

## Run the frontend (dev, Angular on :4200 with API proxy)

```bash
cd frontend/salary-app
npm install
npm start        # ng serve --proxy-config proxy.conf.json  (forwards /api to http://localhost:18080)
```

The dev proxy points at port **18080**, so run the backend on 18080 while
developing (see above). Open http://localhost:4200/.

## Production build (frontend + API in one jar)

```bash
bash scripts/build-frontend.sh   # ng build, copies dist into backend/src/main/resources/static/
cd backend
bash ../scripts/mvn.sh package -DskipTests
java -jar target/salary-management-1.0.0.jar   # serves the Angular app at / and the API at /api on :8080
```

`backend/src/main/resources/static/` is a build artefact (git-ignored);
always run `build-frontend.sh` before packaging.

## Tests

```bash
cd backend
bash ../scripts/mvn.sh test     # 37 tests: validator, stats, insights, CSV parser, full API flow, SPA fallback
```

## What the app does

- **Employees** (`/employees`): paged table (never loads all 10,000 at
  once), search by name, filter by department / country / status, sort,
  page-size selector; row click → detail.
- **Employee detail**: fields, current salary, full salary history (newest
  first), salary revision form (backend 400 validation errors are shown),
  edit and delete (with confirm).
- **Add employee** (`/employees/new`): optional initial salary fields.
- **Insights** (`/dashboard`): average/median pay by department, country
  and role; same-role-across-countries comparison; pay distribution bands
  per currency; headcount by band; CSV export. Figures are always grouped
  by currency — **currencies are never converted**.
- **CSV import** (`/import`): file upload with imported/failed counts and
  per-row errors; sample CSV download
  (`scripts/sample-import.csv`, also served at `/sample-import.csv`).

## Demo video

Shot-by-shot script + Playwright recorder: `demo/demo-script.md`,
`demo/record-demo.mjs`. **Not yet recorded** — the Chromium download
failed in this sandbox (see `demo/demo-script.md` for the status and the
exact retry commands). The script drives the real running app against a
scratch DB and writes `demo/demo-video.mp4`.

## Deployment

See `DEPLOYMENT.md`. One jar, zero setup: Render free tier (`render.yaml`),
any Docker host (`Dockerfile`), or plain `java -jar`. No deployment is
live yet — every host needs an account the deployer creates; the image
ships the seeded demo DB, and free-tier disks are ephemeral (edits are
lost on restart — documented in `DEPLOYMENT.md`).
