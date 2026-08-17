import { defineConfig, devices } from '@playwright/test'

const javaHome = process.env.JAVA_HOME ?? '/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 2 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://127.0.0.1:4173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: './mvnw spring-boot:run -Dspring-boot.run.profiles=e2e',
      cwd: '../vitral-backend',
      url: 'http://127.0.0.1:8081/api/v1/categorias',
      timeout: 120_000,
      reuseExistingServer: false,
      env: {
        JAVA_HOME: javaHome,
        JWT_SECRET: 'MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=',
        JWT_EXPIRATION: '86400000',
        PATH: `${javaHome}/bin:${process.env.PATH ?? ''}`,
      },
    },
    {
      command: 'VITE_API_URL=http://127.0.0.1:8081 npm run dev -- --host 127.0.0.1 --port 4173',
      url: 'http://127.0.0.1:4173',
      timeout: 60_000,
      reuseExistingServer: false,
    },
  ],
})
