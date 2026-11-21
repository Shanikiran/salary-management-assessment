# Trade-offs & Performance Notes

## Deliberate exclusions (from the requirements doc, with reasoning)

| Excluded | Why |
|---|---|
| Payroll processing / tax / payslips | The brief is salary *management* (records + insights), not payroll. Tax rules differ per country and would be a second product. |
| Live FX conversion | No FX source is named in the brief; converting with a hardcoded or scraped rate would fabricate numbers. All analytics are per-currency. |
| Multi-role auth (HR vs employee vs admin) | One persona (HR Manager) is named in the brief. Auth would add a login system, sessions, and password storage with no requirement behind it. |
| Realtime collaboration / locking | Single HR Manager persona; concurrent-edit conflicts are out of scope. Last-write-wins on employee edits, with `updated_at` as the trace. |
| Monthly ↔ annual normalisation in analytics | Seed data is all ANNUAL; manual MONTHLY entries are grouped with annuals by currency and labelled by frequency. Normalising (×12) would silently change what the HR Manager entered. If monthly entries become common, add an `annualised_pay` computed column instead of rewriting history. |

## Performance at 10,000 rows (measured live, 20 Nov 2026)

Backend on localhost, seeded DB (10,000 employees, 12,528 salary records):

| Operation | Time |
|---|---|
| `GET /api/employees?size=20` (page 0) | ~0.19 s (first hit; ~0.01–0.03 s warm) |
| `GET /api/employees?page=499&size=20` (deep page) | ~0.008 s |
| Name search (`?name=Aarav`) | ~0.03 s |
| Department + country filter | ~0.18 s (first hit) |
| `GET /api/employees/EMP-00001` (detail + history) | ~0.02 s |
| `GET /api/insights/by-department` (72 groups) | ~0.44 s |
| `GET /api/insights/by-country` (10 groups) | ~0.15 s |
| CSV import, 7 rows | < 0.5 s |
| Seed script, full 10,000 employees | ~1.0–1.7 s |

Indexes (in `schema.sql`): `employees(name)`, `employees(department,
country, status)`, `salary_records(employee_id, effective_from)`,
`salary_records(effective_from)`. The insights endpoints do one full scan
of current pay (12,528 rows) in Java and aggregate in memory — ~0.4 s is
fine for a dashboard, and avoids pushing median/percentile math into SQL
(SQLite has no `MEDIAN`/`PERCENTILE_CONT`).

## What would break first, and the fix

1. **Deep pagination (`OFFSET`)** — `OFFSET 9980` is fast at 10k rows but
   degrades linearly. Past ~100k rows, switch the list to keyset pagination
   (`WHERE (name, id) > (?, ?) ORDER BY name, id LIMIT ?`).
2. **Insights full scan** — at ~1M salary rows the in-memory aggregation
   would take seconds. Fix: pre-aggregate per (group, currency) into a
   summary table refreshed on write, or move the median/percentile into the
   database (PostgreSQL `percentile_cont`).
3. **SQLite single writer** — concurrent HR Managers editing at once would
   serialise on the write lock. The migration path is PostgreSQL:
   `spring.datasource.url` + the same JdbcTemplate code works unchanged
   (the SQL is standard; only `AUTOINCREMENT` and the `NUMERIC` handling
   need a glance). No code change in the service layer.
4. **`MAX(id)+1` employee numbering** — fine for a single writer, racy
   under concurrency. Under PostgreSQL, use a sequence.

## Testing

35 tests, ~21 s (`scripts/mvn.sh test`): 12 validator, 6 stats, 4
insights (mocked repo), 4 CSV parser, 3 date-parsing, 6 API integration
tests against a real temporary SQLite DB (full flow, validation rejections,
pagination, CSV import incl. the no-half-created-rows assertion, insights
+ CSV export). Deterministic: no clocks (tests pass explicit dates), no
randomness, seed script pinned to `random.seed(42)`.

## Two bugs found by end-to-end testing (not by unit tests)

1. **Seed wrote ISO datetimes in `created_at`/`updated_at`** while the row
   mappers parsed plain dates → every list/detail call 500'd against the
   seeded DB. Fixed both sides: mappers now tolerate datetimes
   (`SqlDates`), and the seed writes plain dates. Lesson: unit tests used
   API-created rows (plain dates); only the live seeded DB exposed it.
2. **CSV import created the employee before validating the salary** →
   rejected rows left half-created employees. Fixed by validating first
   and rolling back on insert failure; covered by an integration test
   asserting the employee count after a mixed import.
