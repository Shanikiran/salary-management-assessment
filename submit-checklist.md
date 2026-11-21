# Incubyte assessment — submit checklist (20 Nov 2026)

Build is done and verified (Spring Boot + Angular, exactly 10,000 employees,
37 tests passing, 16 commits + 2 fix/doc commits). Nothing goes to Sandli
until you have reviewed it. Three steps left, in this order — about
30–40 minutes total, most of it waiting on the Render build.

## Step 1 — Put the repo on GitHub (~5 min)

The repo is local-only (branch `master`, no remote yet). It lives on this
machine, so the push has to happen from here with your GitHub account.

1. Create an empty repo on github.com/new — suggested name
   `salary-management-assessment`, **Public** (so Sandli can open it with
   no access request; Private works too but then you must grant her access).
   Do NOT tick "Add a README" — the repo already has one.
2. Share the repo URL with Muse (e.g.
   `https://github.com/<your-username>/salary-management-assessment`).
   Muse will add it as the remote and push:
   `git remote add origin <url>` + `git push -u origin master`.
   Your GitHub sign-in will be needed once (browser takeover, same as
   other applies) — Muse will ask for it, nothing is sent without you.
3. Alternative if you would rather do it yourself: a ready-to-upload zip
   of the repo is at
   `~/workspace/goals/10-lpa-full-stack-job-search/files/incubyte-assessment-repo-20261120.zip`
   — upload its contents to the empty GitHub repo via the web UI.

Fixed 20 Nov ~10:15 IST — check before you deploy: the seeded DB
(`backend/data/salary.db`, 10,000 employees) is now tracked in git and the
Dockerfile copies it into the runtime image. Before the fix, the Render
build would have failed (the DB was gitignored, so `COPY
backend/data/salary.db` had no source file) and, even if it had built,
the live app would have started with 0 employees (the jar does not
contain the DB). Verified: `employees: 10000, salary_records: 12528`.
Docker itself was NOT built here (no Docker daemon in this sandbox), so
treat the first Render build as the verification — if it errors, send
Muse the log.

## Step 2 — Deploy free on Render (~10 min + build time)

Needs a free Render account (yours — no account exists yet, no card needed).

1. Sign up at render.com (email or "Sign up with GitHub").
2. New → Web Service → pick the `salary-management-assessment` repo.
   Render detects `render.yaml` (Docker build, free plan, health check
   `/api/health`). Leave the defaults, click Deploy.
3. Wait for the build (several minutes: it runs `npm ci`, `ng build`,
   then `mvn package`). When it says Live, verify:
   `<your-url>/api/health` → `{"status":"UP"}`, and open `<your-url>/` —
   the employee list should show 10,000 total.
4. Copy the Render URL — it is the "Live app" link for the email.

Note: free-tier disks are ephemeral — any employee/salary change made
while demoing is lost on restart, and the app comes back to the 10,000
seed. That is fine for the demo.

## Step 3 — Record the demo video (~10 min)

The automated recorder cannot run in this sandbox (Playwright's Chromium
download fails twice with the same error, and the system Chromium at
`/opt/meta-chromium/chrome` refuses to start here — "Hatch WebGL domain
policy is required"). Record it yourself against the LIVE Render URL
from Step 2 — no local setup needed.

- Any screen recorder: Windows Win+G or Snipping Tool video, macOS
  Cmd+Shift+5. ~90 seconds, silent walkthrough (no voiceover needed).
- Follow the 8-shot checklist in `demo/demo-script.md`: employee list
  (10,000 total) → search "Aarav" (143 matches) → EMP-00001 detail +
  history → add revision ₹13,20,000 from 2026-12-01 → rejected revision
  −₹5,000 (red backend error) → dashboard → same-role-across-countries
  → CSV import (5 imported, 2 failed, per-row reasons).
- Upload the video (Drive, or YouTube Unlisted) and copy the share link —
  it is the "Demo video" link for the email.

## Step 4 — Send the email (~2 min)

Reply to Sandli Srivastava's assessment email (19 Nov, 00:42 IST) from
`shanikirannimbalkar@gmail.com`. The draft is ready at
`submission-email-draft.md` — fill the three links (live app, repo,
demo video) and send. Do not send it before all three links work when
opened in a fresh browser tab.
