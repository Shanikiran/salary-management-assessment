# AI-Prompt Log

The brief asks for the prompts/instructions used while building this
solution, so they are recorded here honestly. Entries are in chronological
order. Verbatim quotes are marked as such; everything else is a faithful
summary of the instruction as given. No prompt has been edited to make the
build look better than it was — including the two bugs found by end-to-end
testing (see `tradeoffs-performance.md`).

## 1. The candidate's go-ahead (Shanikiran, 20 Nov 2026 ~08:06 IST)

Verbatim (his message, typos as sent):

> "Incubator assemetmtbgo ahead"

i.e. go ahead with the Incubyte take-home assessment. This is the only
instruction from the candidate during the build. He did not dictate any
technical choice; stack and design follow the brief + the approved
requirements draft.

## 2. Build task from the parent agent (20 Nov 2026, start of this session)

Verbatim task brief (abridged only for the file paths, which are in the
repo): build the complete solution end-to-end in
`~/workspace/goals/10-lpa-full-stack-job-search/files/incubyte-assessment/`,
from the approved requirements draft and the original brief docx — final
requirements doc (FINAL, same scope/exclusions); Java 17 + Spring Boot
REST API on SQLite (zero-setup, brief's named example); endpoints for
employee CRUD + search/filter/pagination, CSV bulk import with row-level
errors, append-only salary history, revisions with effective dates,
validation (no negative amounts, no overlapping effective periods,
currency required), analytics (average/median by department/country/role,
distribution, headcount by band, same-role-across-countries with currency
as stored), CSV export; Angular UI (list/search, detail + history,
revision form, analytics dashboard); seed script for 10,000 employees in
one command; unit tests on core logic; design + trade-off/performance
notes; this AI-prompt log; incremental git commits (requirements →
skeleton → employees → salaries → insights → tests → deploy-ready);
verify everything by running it before reporting; attempt deployment only
via free no-credit-card routes, otherwise report what is needed; demo
video: record if possible, otherwise a shot-by-shot script + commands.
Standing rules: do not invent claims about Shanikiran's experience; do not
email anyone; submission only after he reviews the finished build.

## 3. Requirements draft (19 Nov 2026, parent session)

The requirements draft
(`incubyte-assessment-requirements-draft-20261119.md`, promoted to
`requirements.md`) was written from the original brief
("Salary Management Assessment - Candidates.docx", read in full from
SharePoint) under the instruction: one-page requirements doc covering
goal, scope & features, deliberate exclusions + reasoning, for Employee
Management, Salary Management, and Salary Insights (Analytics). Proposed
stack in the draft: Spring Boot + Angular. Exclusions proposed in the
draft: no payroll processing/tax, no live FX conversion, no multi-role
auth, no realtime collaboration. The draft was approved before this build
session started; the build implements it as written.

## 4. Seed-script delegation (child subagent, 20 Nov 2026 ~02:30 IST)

Instruction given to a child subagent (verbatim substance): write
`scripts/seed.py` (Python 3 stdlib only) + `scripts/seed.sh` (one-command
wrapper) + `scripts/sample-import.csv` (7 rows: 5 valid, 2 invalid — one
negative salary, one empty currency). Requirements given: deterministic
(`random.seed(42)`), idempotent (second run exits 0 without duplicating),
`--reset` flag, `--db <path>` override, default DB
`backend/data/salary.db`; exactly 10,000 employees EMP-00001…EMP-10000
across 8 departments and 10 countries with realistic names per country;
salaries in local currency, annual, within per-country ranges; ~25% of
employees get a second salary record, ~5% a third (revisions 5–15% up,
6–18 months later, never future-dated); statuses ~90% ACTIVE / ~5%
ON_LEAVE / ~5% EXITED; joining dates 2018 → today; schema taken from
`backend/src/main/resources/schema.sql`; verify by querying the DB back
(counts, no duplicates, no invalid amounts, no future dates, per-country
counts). The child delivered it; verification numbers are in the README /
`tradeoffs-performance.md`. One defect in the child's output was found and
fixed by the coordinator afterwards (see entry 8).

## 5. Backend build (coordinator, 20 Nov 2026 ~02:37–03:00 IST)

No separate prompt: the coordinator implemented the backend directly from
the task brief (entry 2) and the requirements doc. Toolchain installed
from scratch in the sandbox (Temurin JDK 17, Maven 3.9.9, Node 24).
Notable debugging, driven by tool output rather than new prompts:

- Maven could not reach the internet through the sandbox egress proxy
  (`hatch-egress-proxy:3128`, credentials rotate every shell invocation,
  never hardcoded). Fixes applied in order: settings.xml written to the
  wrong home (Maven reads `/root/.m2/settings.xml`, not
  `/home/hatch/.m2/settings.xml`); plain-HTTP Google Maven Central mirror
  (repo1.maven.org 301s http→https); root cause — `/etc/hosts` maps the
  proxy to both IPv4 (real) and IPv6 (garbage to Java's HTTP stack; Java
  prefers IPv6) — fixed with `-Djava.net.preferIPv4Stack=true`.
  Preserved as the reproducible `scripts/mvn.sh` wrapper, which regenerates
  both settings.xml files from the current env credentials on every run.
- Compile error `SalaryRepository.java:[49,53] ';' expected`: a text block
  opened as `"""SELECT ...` on one line — Java requires the opening `"""`
  to be followed by a line terminator. Fixed by moving the SQL to the next
  line.
- Test failures (5 of 32 on first run): 2 were wrong test expectations
  (median of {1,2,3} is 2, not 3; `XXX` is a *valid* ISO 4217 code — tests
  now use `ZZZ`); 3 were test-isolation issues (shared temp DB across test
  methods — fixed with a `@BeforeEach` table cleanup).

## 6. End-to-end verification (coordinator, 20 Nov 2026 ~02:52–03:10 IST)

No separate prompt: the task brief (entry 2) required exercising every
endpoint live before reporting. The coordinator booted the app against the
seeded DB and curled every endpoint (list/search/filter/deep page,
detail, create/revision/history, validation rejections, update/delete,
all five insight endpoints, CSV import + export), timing each. This found
two real bugs (see `tradeoffs-performance.md`): the seed's ISO-datetime
`created_at` values 500ing every list call, and CSV import leaving
half-created employees on rejected rows. Both fixed, covered by new tests,
and re-verified live (35 tests green; import of the sample file now adds
exactly 5 employees).

## 7. Frontend delegation (child subagent, 20 Nov 2026 ~03:10 IST)

Instruction given to a child subagent (verbatim substance): build the
Angular 20 frontend in `frontend/` — scaffold with npx @angular/cli
(Node 24 installed, no CLI yet; proxy note from entry 5 repeated for npm);
pages for employee list/search (paged, filtered), detail + salary history,
revision form (showing backend 400 errors), add-employee form, analytics
dashboard (per-currency tables, no conversion, role comparison, percentile
distribution, headcount by band), CSV import with per-row error display
and sample/exports; dev proxy to the running backend on port 18080;
production build copied to `backend/src/main/resources/static/` via a new
`scripts/build-frontend.sh`; README at repo root (prerequisites, seed,
backend run, frontend dev run, production build, tests); verify by
running it (dev server + packaged jar on 8080 serving UI and API);
incremental git commits; do not touch backend code or files outside the
repo. The full API contract (endpoints, request/response shapes, error
format) was pasted into the instruction verbatim from the backend code.

## 8. Fixes applied by the coordinator after the child's seed delivery

- Seed wrote `created_at`/`updated_at` as ISO datetimes
  (`2026-04-05T09:00:00Z`) while the Java row mappers parsed plain dates.
  Fix (both sides): mappers now tolerate datetimes via `SqlDates`
  (date part), and the seed writes plain dates. DB re-seeded; counts
  unchanged (10,000 employees, 12,528 salary records), new SHA-256 prefix
  `8754795b8fc9f7cc`.
- No other AI tools were used. No code was generated by pasting from
  external sources; all code was written in this session from the brief
  and the requirements doc.

## 9. Frontend delivery + SPA fallback fix (coordinator, 20 Nov 2026 ~03:11–03:20 IST)

- The frontend child (entry 7) delivered the Angular 20 app, verified by
  curl through the dev proxy plus the exact API calls the UI issues, and
  by serving the packaged jar. One incident during its testing: its
  write-flow test hit the wrong employee id (EMP-10001 instead of its own
  EMP-10006) and overwrote a sample-imported employee; the child repaired
  it from the CSV + DB evidence and the coordinator verified the repair
  (EMP-10001 = Aarav Sharma, 1,200,000 INR ANNUAL from 2023-04-10; totals
  10,005 employees / 12,533 salary records — the 5 extra are the sample
  import from the coordinator's own live verification, entry 6).
- The child flagged a real gap it was told not to fix (backend code):
  direct loads of `/dashboard`, `/employees`, `/import`, etc. returned 404
  from the jar. The coordinator implemented `SpaController` (forward
  non-API, extension-less paths to `/index.html`; one method per path
  depth, since Spring's path parser forbids anything after `**` — the
  first `/**/{path}` attempt failed context startup with
  `PatternParseException` and was rewritten). 2 new tests; full suite now
  37 green. Verified live against the packaged jar: `/`, `/dashboard`,
  `/employees`, `/employees/EMP-00001`, `/employees/EMP-00001/edit`,
  `/import` all 200 with the Angular shell; main bundle 200; API intact.
- Demo video: `npx playwright install chromium` attempted 20 Nov 2026;
  the browser download failed ("Download failure, code=1") while a
  sandbox approval request for the action timed out, so it was NOT
  retried. The CDN is reachable (307/200 on direct curl), so the failure
  looks transient. Deliverables instead: `demo/record-demo.mjs`
  (Playwright recorder, real app, scratch DB) + `demo/demo-script.md`
  (8-shot script with captions). The video itself is NOT recorded.
- Deployment: no Docker daemon exists in this sandbox, so the Dockerfile
  was written but NOT built locally; `render.yaml`, `Procfile`, and
  `DEPLOYMENT.md` document the free-tier path and its ephemeral-disk
  caveat honestly. No account was created anywhere; nothing is deployed
  live.
