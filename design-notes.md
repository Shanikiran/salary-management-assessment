# Design Notes — Salary Management System

Incubyte take-home assessment. Stack: Java 17 + Spring Boot 3.3.5, SQLite
(via `spring-boot-starter-jdbc` + `JdbcTemplate`), Angular 20 UI. One jar
serves the API and the Angular production build (`classpath:/static/`).

## Domain model

Two tables, no ORM:

```sql
employees (id TEXT PK, name, department, role, country, city,
           joining_date TEXT, status TEXT, created_at TEXT, updated_at TEXT)
salary_records (id INTEGER PK AUTOINCREMENT, employee_id TEXT FK,
           base_pay NUMERIC, currency TEXT, pay_frequency TEXT,
           effective_from TEXT, created_at TEXT,
           UNIQUE (employee_id, effective_from))
```

- `id` is a human-readable `EMP-00001`… sequence, assigned as
  `MAX(numeric part) + 1`. Readable in URLs, the demo video, and the
  assessment discussion; no UUIDs to copy-paste.
- All dates are stored as ISO `YYYY-MM-DD` text. The row mappers also accept
  ISO datetimes (`2024-03-01T09:00:00Z`) by taking the date part — the seed
  script originally wrote datetimes and the first end-to-end run 500'd on
  `LocalDate.parse`; the tolerant parser (`SqlDates`) is the fix, and the
  seed now writes plain dates.
- Money is `NUMERIC` in SQLite and `BigDecimal` in Java. Never `double`:
  binary floating point cannot represent most decimal salaries exactly.

## Salary history is append-only

A salary revision is a **new row**, never an `UPDATE`. Deleting an employee
removes their salary rows (application-level cascade in
`EmployeeService.delete`); salary rows are never edited or deleted on their
own. Consequences:

- Full history is always available for the detail page and for audit.
- "Current pay" is defined, not stored: the record with the latest
  `effective_from` on or before **today**. A future-dated revision becomes
  current only when its date arrives — the UI can show it as "upcoming"
  because history is ordered newest-first and the current record is
  highlighted by comparing `effective_from <= today`.
- Overlap rule: a new `effective_from` must be **strictly after** the latest
  existing one. Periods run from `effective_from` until the next record's
  `effective_from`, so an in-between date would split a period. A duplicate
  date is blocked twice: by validation and by the
  `UNIQUE (employee_id, effective_from)` constraint.
- The revision form therefore only ever asks for a future-dated change; the
  validator rejects anything else with a plain-language message.

## Validation (single source of truth)

`SalaryValidator` is pure static code with no Spring dependency, so it is
unit-tested directly (12 tests) and reused by the API and by CSV import:

- Employee: name/department/role/country required, joining date not in the
  future, status in `ACTIVE | ON_LEAVE | EXITED`.
- Salary: amount > 0, currency a real ISO 4217 code (`Currency.getInstance`
  — note `XXX` is a *valid* ISO code, "no currency", so tests use `ZZZ`),
  frequency `MONTHLY | ANNUAL`, effective date after the latest existing
  record and not a duplicate.
- The API returns `400 {"error":"validation failed","details":[...]}`; the
  Angular UI renders `details` verbatim.

## Analytics: per currency, never converted

The brief names no FX source, and inventing exchange rates would fabricate
numbers. Every aggregate is grouped by `(group, currency)`:

- `by-department`, `by-country`, `by-role`: headcount, average, median,
  min, max per group+currency. Median/average use `BigDecimal` with
  `HALF_UP` rounding to 2 decimals.
- `compare-role?role=X`: the same stats grouped by country for one role —
  the "same role across countries" view, each country's own currency shown.
- `distribution`: percentile bands **computed from the data itself** per
  currency — P25/P50/P75/P90, five bands (Bottom 25%, 25–50%, 50–75%,
  75–90%, Top 10%). Fixed thresholds (e.g. "under ₹10L") mean different
  things in INR vs JPY, so data-derived bands are the honest choice.
  `headcount-by-band` is the same data as a `currency → bands` map.
- All analytics run over **current pay only** (latest record on/before
  today). Historical revisions do not pollute the averages.
- Seed salaries are all `ANNUAL` in local currency, so within-currency
  comparisons are apples-to-apples. The API still accepts `MONTHLY` for
  manual entries; monthly and annual figures are grouped together by
  currency and labelled by their frequency in the detail view — a known
  simplification, documented in `tradeoffs-performance.md`.

## Pagination

The list endpoint never returns all 10,000 rows: `size` defaults to 20,
max 100, and the response carries `totalElements`/`totalPages` so the UI
can page. Search is `name LIKE %q%` (case-insensitive); filters are exact
matches on department/country/status. Sorting is by `name` by default
(`name, department, country, role, joiningDate` allowed — the sort column
is allow-listed in code, never interpolated from the request).

## CSV import (the Excel-replacement path)

`POST /api/employees/import`, multipart file, header:
`name,department,role,country,city,joining_date,status,base_pay,currency,pay_frequency,effective_from`.
Row-by-row, one bad row never blocks the rest; every failure is reported
as `{row, reason}` with the file's 1-based row number. The parser is a
hand-rolled RFC-4180 line parser (quoted fields, embedded commas, escaped
quotes) — no extra dependency for a 40-line job.

Important correctness fix found during end-to-end testing: the first version
created the employee **before** validating the salary, so a rejected row
left a half-created employee with no salary behind (live proof: an import
of the sample file created 7 employees for 5 valid rows). The import now
validates the salary **before** creating the employee, and rolls the row
back (deletes the just-created employee) if the insert itself fails. A
rejected row leaves nothing behind — verified live (10,005 employees after
importing the 7-row sample: exactly 5 added) and by an integration test
that asserts the employee count after a mixed import.

## Why JdbcTemplate + SQLite, not JPA/PostgreSQL

- The brief names SQLite as the example and the deliverable must run with
  zero setup. SQLite is a single file (`./data/salary.db`); the schema is
  applied from `schema.sql` on startup; there is nothing to install.
- JdbcTemplate keeps the SQL explicit. With JPA on SQLite you fight dialect
  gaps (no `BOOLEAN`, weak `ALTER TABLE`, `AUTOINCREMENT` quirks) for zero
  benefit at this scale — the queries here are five hand-written statements.
- The trade-off is real: no connection pooling benefit beyond Hikari's
  default, no migrations framework (schema is created once via
  `spring.sql.init`), and SQLite allows a single writer. All acceptable for
  a 10k-row single-HR-manager tool; see `tradeoffs-performance.md` for the
  migration path if it ever outgrows SQLite.

## Layering

```
web/        controllers only: HTTP in, JSON out, status codes, error mapping
service/    EmployeeService, SalaryService, InsightsService, CsvService, Stats
repo/       EmployeeRepository, SalaryRepository (JdbcTemplate, explicit SQL)
model/      Employee, SalaryRecord (records)
validation/ SalaryValidator (pure, unit-tested)
```

Controllers never touch the database; repositories never validate business
rules; `Stats` (median/average/percentile) is pure and unit-tested.
