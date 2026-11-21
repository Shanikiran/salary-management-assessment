# Incubyte Assessment — Requirements (FINAL)

**Role:** Software Craftsperson (Java/Angular) II — Incubyte · **Brief:** "Assessment with Product Framing" (Salary Management Assessment – Candidates.docx, 3 pages, received 19 Nov 2026 00:42 IST from Sandli Srivastava, sandli@incubyte.co)
**Assessment link (from the email):** https://insights.zohorecruit.in/ck1/2518b.df847630b/fb0c8a40-e383-11f1-8f51-8e9a6c33ddc2/95832a806fa895cc4f93d86de691929dd1dda676/2?e=lbhbItISILCXY9kgRnhDvfytLvdcf23ETufLZRIj%2BgBODTDFDT38hkZ%2Fco4NHeZ%2FlNIgxxyroFfHZTZMpykSPD9sxCLUIM7VuoMFFN9kk1rwA6atGfSHekxqiVZoSo42ng%2Fnkf%2BE4qi5bKa8Ri%2B6H1A4tViFy4%2Bo%2BsC%2FBkgUpN8%3D
**Status:** FINAL — approved by Shanikiran; go-ahead given 20 Nov 2026. Built as submitted.

## Goal
Replace ACME org's Excel-based salary handling with web software so one HR Manager can manage pay for 10,000 employees across multiple countries and answer "how do we pay people?" questions quickly and reliably.

## User
HR Manager (single persona). One role, one permission level for this assessment — no multi-role access control.

## Scope & features

**1. Employee Management**
- CRUD for employees: name, employee ID, department, role/title, location/country, joining date, employment status.
- Search/filter by name, department, country, status; paginated list (10,000 rows must never load at once).
- Bulk import from CSV (the realistic migration path off Excel) with row-level error reporting.

**2. Salary Management**
- Salary record per employee: base pay, currency, pay frequency, effective-from date, history of changes (every change kept, never overwritten — pay history is audit-sensitive).
- Salary revisions with effective dates; view current vs historical pay.
- Validation: no negative amounts, no overlapping effective periods, currency required per record.

**3. Salary Insights (Analytics)**
- Dashboard: average/median pay by department, by country, by role; pay distribution; headcount by band.
- Comparison views (e.g. same role across countries, with currency shown as stored — see exclusions).
- Export of insights tables to CSV.

## Technical choices (to confirm)
- Backend: Java + Spring Boot (per the role applied for), REST API, relational DB — SQLite for zero-setup portability (brief's example) or PostgreSQL if deployed.
- UI: Angular (per the role; the brief lists Angular for Java) + a component library.
- Seeding: script generating 10,000 realistic employees across departments/countries.
- Tests: unit tests on core logic (salary history, validation, analytics calculations) — fast, deterministic.
- Readiness: deployed app + short video demo, as the brief asks.

## Deliberately OUT (with reasoning)
- **Payroll processing / payslips / tax / bank transfers** — the brief is salary *management and insight*, not running payroll; tax rules differ per country and would dominate the build.
- **Live currency conversion** — no FX source is specified; store currency as entered and show it explicitly, rather than converting with a guessed rate.
- **Multi-role auth / SSO** — single HR Manager persona; auth beyond a simple login adds scope without demonstrating the core judgement.
- **Real-time collaboration / notifications** — not needed for one HR Manager managing records.

## Development approach
- Build with agentic AI tools, keeping a log of prompts/instructions used.
- Incremental commits showing the solution evolving (requirements → skeleton → employees → salaries → insights → tests → deploy).
- Artifacts in the repo: this requirements doc, design notes, trade-off explanations, AI-prompt log.

## Submission
- Commit to a Git repo; reply to Sandli's email with the repo link when done.
- No deadline is stated in the brief — but the email says fast submissions get prioritised, so aim to submit as soon as the build is solid, not to perfect it.
- After submission: technical discussion rounds on design decisions, architecture, AI workflows and trade-offs — be ready to walk through this doc and the commit history.
