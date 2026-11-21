/**
 * Demo-video recorder for the Incubyte salary-management assessment.
 *
 * Records a real walkthrough of the RUNNING app (no mocks) with Playwright:
 *   1. employee list + search + pagination
 *   2. employee detail + salary history
 *   3. valid salary revision (history grows)
 *   4. invalid revision (backend 400 shown in the UI)
 *   5. analytics dashboard (by-department + same-role-across-countries)
 *   6. CSV import of the sample file (5 imported / 2 failed)
 *
 * SETUP (one time):
 *   npm install playwright
 *   npx playwright install chromium     # downloads the browser (~150 MB)
 *
 * RUN (against a SCRATCH database so the demo's edits never touch the
 * deliverable DB):
 *   bash scripts/seed.sh --reset --db /tmp/demo-salary.db
 *   cd backend
 *   DB_PATH=/tmp/demo-salary.db java -jar target/salary-management-1.0.0.jar --server.port=8080
 *   # in another shell:
 *   node demo/record-demo.mjs            # writes demo/demo-video.webm + demo/shots/*.png
 *   ffmpeg -y -i demo/demo-video.webm -c:v libx264 -pix_fmt yuv420p demo/demo-video.mp4
 *
 * BASE_URL env var overrides the default http://localhost:8080.
 */
import { chromium } from 'playwright';
import { mkdirSync } from 'node:fs';

const BASE = process.env.BASE_URL ?? 'http://localhost:8080';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

mkdirSync('demo/shots', { recursive: true });

const browser = await chromium.launch();
const context = await browser.newContext({
  viewport: { width: 1280, height: 720 },
  recordVideo: { dir: 'demo', size: { width: 1280, height: 720 } },
});
const page = await context.newPage();

const shot = async (name) => {
  await page.screenshot({ path: `demo/shots/${name}.png` });
  await sleep(800); // let the shot "breathe" on video
};

// 1 — Employee list, search, pagination
await page.goto(`${BASE}/employees`);
await page.waitForSelector('table tbody tr');
await shot('01-list');
await page.fill('input[placeholder*="Search" i]', 'Aarav');
await sleep(1200);
await shot('02-search-aarav');
await page.fill('input[placeholder*="Search" i]', '');
await sleep(600);

// 2 — Employee detail + salary history
await page.goto(`${BASE}/employees/EMP-00001`);
await page.waitForSelector('table tbody tr');
await shot('03-detail-history');

// 3 — Valid salary revision (history grows by one)
await page.fill('input[name="basePay"]', '1320000');
await page.fill('input[name="effectiveFrom"]', '2026-12-01');
await page.click('button[type="submit"]');
await sleep(1200);
await shot('04-revision-added');

// 4 — Invalid revision: negative amount, backend 400 shown in the UI
await page.fill('input[name="basePay"]', '-5000');
await page.fill('input[name="effectiveFrom"]', '2027-01-01');
await page.click('button[type="submit"]');
await sleep(1200);
await shot('05-revision-rejected');

// 5 — Analytics dashboard
await page.goto(`${BASE}/dashboard`);
await page.waitForSelector('table tbody tr');
await shot('06-dashboard');
await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight / 2));
await sleep(800);
await shot('07-dashboard-compare-role');

// 6 — CSV import of the sample file
await page.goto(`${BASE}/import`);
await page.setInputFiles('input[type="file"]', 'scripts/sample-import.csv');
await page.click('button[type="submit"]');
await sleep(1500);
await shot('08-csv-import-result');

await context.close();
await browser.close();
console.log('Done. Video: demo/demo-video.webm (convert with ffmpeg, see header).');
