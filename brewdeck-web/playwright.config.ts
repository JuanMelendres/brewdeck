import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests (ADR-022). They need the API, Postgres and Mailpit running; see
 * docs/testing/e2e-testing.md.
 */
const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:3000';
const isCI = Boolean(process.env.CI);

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: isCI,
  retries: isCI ? 1 : 0,
  // A stuck run fails fast instead of eating the job's 20 minutes.
  globalTimeout: isCI ? 5 * 60_000 : undefined,
  workers: isCI ? 2 : undefined,
  reporter: isCI ? [['github'], ['html', { open: 'never' }]] : [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    locale: 'es-MX',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'phone', use: { ...devices['Pixel 7'] } },
  ],
  // Locally Playwright starts the web if it is not running. CI starts it in its own step: when
  // Playwright owned `pnpm start`, the run hung for minutes at the end, waiting for next-server
  // (which detaches from pnpm) to exit.
  webServer: {
    command: 'pnpm start',
    url: baseURL,
    reuseExistingServer: true,
    timeout: 120_000,
  },
});
