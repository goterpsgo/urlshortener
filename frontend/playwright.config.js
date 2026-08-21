import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
  },
  webServer: [
    {
      command: 'npm run dev',
      url: 'http://localhost:5173/app/login',
      reuseExistingServer: true,
      timeout: 30_000,
    },
    {
      command: './mvnw spring-boot:run',
      cwd: '..',
      port: 8080,
      reuseExistingServer: true,
      timeout: 60_000,
    },
  ],
})
