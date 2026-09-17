import { defineConfig, devices } from '@playwright/test'

/** 双视口（engineering-plan §4.1：375px 移动 + 1280px 桌面）；webkit 列 M1 后补 */
export default defineConfig({
  testDir: './tests/e2e',
  timeout: 30_000,
  retries: 0,
  use: { baseURL: 'http://localhost:5173' },
  webServer: [
    { command: 'cd ../server && java -jar target/server-0.1.0-SNAPSHOT.jar', port: 8080, reuseExistingServer: true },
    { command: 'pnpm dev', port: 5173, reuseExistingServer: true },
  ],
  projects: [
    { name: 'mobile-375', use: { viewport: { width: 375, height: 812 } } },
    { name: 'desktop-1280', use: { viewport: { width: 1280, height: 800 } } },
  ],
})
