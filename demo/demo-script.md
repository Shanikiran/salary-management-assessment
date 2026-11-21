# Demo video — shot-by-shot script

Target: ~90 seconds, silent walkthrough with on-screen captions (no
voiceover needed — the UI shows everything). Record with
`demo/record-demo.mjs` (Playwright, real app, scratch DB — see the header
of that file), then convert to mp4 with ffmpeg. If the browser cannot be
installed where you are, these shots are the exact manual checklist: open
the running app and perform each step while screen-recording.

| # | Shot (file) | What to show | Caption |
|---|---|---|---|
| 1 | `01-list` | Employee list, page 1 of 500, 10,000 total | "10,000 employees — paged, never loaded all at once" |
| 2 | `02-search-aarav` | Search "Aarav" → 143 matches | "Search + filters, instant" |
| 3 | `03-detail-history` | EMP-00001 detail: current salary + history | "Every pay change kept — history is append-only" |
| 4 | `04-revision-added` | Add revision 1,320,000 INR from 2026-12-01 → history grows, newest first | "Salary revision with effective date" |
| 5 | `05-revision-rejected` | Revision −5,000 → red backend error shown | "Validation: no negative amounts, no overlapping periods" |
| 6 | `06-dashboard` | Dashboard: avg/median by department, currency on every row | "Analytics per currency — never converted" |
| 7 | `07-dashboard-compare-role` | Same-role-across-countries comparison | "Same role, 10 countries, each in its own currency" |
| 8 | `08-csv-import-result` | CSV import of the sample file → 5 imported, 2 failed, per-row reasons | "CSV import: bad rows reported, never half-created" |

Closing caption: "Java 17 · Spring Boot · SQLite · Angular 20 — one jar,
zero setup: `java -jar salary-management-1.0.0.jar`".

## Status

**Not yet recorded in this sandbox.** `npx playwright install chromium`
was attempted on 20 Nov 2026 and the browser download failed
("Download failure, code=1") while a sandbox approval request for the
action timed out; the install was **not** retried, per the approval
expiry. Retried 20 Nov ~09:15 IST (Playwright 1.65.1, `npx playwright
install chromium`): failed again with the identical error —
"Failed to download Chrome for Testing 159.0.8154.6 (playwright
chromium v1255), Download failure, code=1" — even though the CDN
itself answers (`cdn.playwright.dev` returns 307). A third check on
20 Nov ~10:15 IST found a system Chromium at /opt/meta-chromium/chrome,
but it also cannot run here: it exits at startup with "Hatch WebGL domain
policy is required for browser startup". The automated
recorder therefore cannot run in this sandbox; record manually on a
machine with a browser, using the shot table above as the checklist.
The jar itself was verified running this morning (`/api/health` →
`{"status":"UP"}`, `totalElements: 10000` on a scratch DB).
