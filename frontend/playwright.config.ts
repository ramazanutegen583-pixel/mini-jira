import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './e2e', fullyParallel: false, workers: 1, retries: process.env['CI'] ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: { baseURL: 'http://127.0.0.1:4202', channel: process.env['PLAYWRIGHT_CHANNEL'], trace: 'retain-on-failure', screenshot: 'only-on-failure', viewport: { width: 1440, height: 1050 } },
  webServer: [
    { command: process.platform === 'win32' ? 'mvnw.cmd -B -ntp spring-boot:run -Dspring-boot.run.profiles=dev' : 'sh mvnw -B -ntp spring-boot:run -Dspring-boot.run.profiles=dev', cwd: '../backend', url: 'http://127.0.0.1:8082/actuator/health', reuseExistingServer: !process.env['CI'], timeout: 180000 },
    { command: 'pnpm start', url: 'http://127.0.0.1:4202', reuseExistingServer: !process.env['CI'], timeout: 120000 }
  ]
});
