import { configDefaults, defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import tsconfigPaths from 'vite-tsconfig-paths';

export default defineConfig({
  plugins: [react(), tsconfigPaths()],
  test: {
    environment: 'jsdom',
    environmentOptions: {
      jsdom: {
        url: 'http://localhost',
      },
    },
    globals: true,
    restoreMocks: true,
    setupFiles: './vitest.setup.ts',
    // e2e/ holds the Playwright suite (ADR-022), run with `pnpm e2e`.
    exclude: [...configDefaults.exclude, 'e2e/**'],
  },
});
