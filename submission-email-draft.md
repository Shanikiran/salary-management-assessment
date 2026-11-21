# Submission email to Sandli — DRAFT (do not send yet)

Ready to paste as a reply to Sandli Srivastava's assessment email
(19 Nov 2026, 00:42 IST) once the three placeholders below are filled.

- **To:** sandli@incubyte.co
- **Subject:** Re: Salary Management Assessment — submission
- **Send from:** shanikirannimbalkar@gmail.com

---

Hi Sandli,

Sharing my completed take-home assessment for the Software Craftsperson
(Java/Angular II) role.

- **Live app:** [PASTE RENDER URL HERE]
- **Repo:** [PASTE GITHUB URL HERE]
- **Demo video (~90 sec):** [PASTE VIDEO LINK HERE]

What I built: a salary-management web app for one HR Manager replacing
Excel, for 10,000 employees. Java 17 + Spring Boot 3.3 backend (REST API,
SQLite, append-only salary history, CSV import with per-row errors) and
an Angular 20 UI (paged employee list, detail + salary revisions,
insights dashboard grouped per currency — never converted).

In the repo: one-page requirements doc, design notes, trade-off and
performance notes, AI-prompt log, and 16 incremental commits. 37 tests
passing. The seeded database holds exactly 10,000 employees, and the
app also runs locally as one jar — `java -jar
salary-management-1.0.0.jar` — no database server or Node needed at
runtime.

Happy to walk through the design decisions, architecture, AI workflow
and trade-offs in the technical discussion.

Thanks,
Shanikiran Nimbalkar
Software Developer | shanikirannimbalkar@gmail.com | +91 7507439761

---

## Still to fill before sending (all three need Shanikiran)

1. **Repo URL** — the repo is local only (16 commits, no remote yet).
   Push it to GitHub from his account, then paste the link.
2. **Live app URL** — no deployment is live yet. Needs a free Render
   account (his): push to GitHub, then Render → New → Web Service →
   pick the repo; `render.yaml` is already in the repo (Docker build,
   free plan, health check `/api/health`). Full steps:
   `DEPLOYMENT.md` (Option A). Note: Render free-tier disks are
   ephemeral — edits made in the demo are lost on restart; the seeded
   10,000-employee DB is baked into the image, so it comes back.
3. **Demo video link** — not recorded yet. The automated recorder
   (`demo/record-demo.mjs`, Playwright) cannot run in this sandbox:
   the Chromium download failed again on 20 Nov (~09:15 IST) with the
   same `Download failure, code=1` as the first attempt, even though
   the CDN answers. Fastest route: record it manually on his own
   machine using the 8-shot checklist in `demo/demo-script.md`
   (~90 seconds, silent walkthrough with captions, no voiceover
   needed) while the app runs locally with
   `java -jar backend/target/salary-management-1.0.0.jar`.

## Verified this morning (20 Nov 2026, ~09:11 IST)

- Built jar runs: `/api/health` → `{"status":"UP"}` and
  `/api/employees?page=0&size=1` → `totalElements: 10000`.
  Ran against a scratch DB (`/tmp/demo-salary.db`); the deliverable
  DB was not touched.
- Jar: `backend/target/salary-management-1.0.0.jar` (35 MB).
- Seeded DB: `backend/data/salary.db` (3.9 MB, 10,000 employees).
