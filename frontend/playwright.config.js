import { defineConfig } from "@playwright/test";
export default defineConfig({
  expect: { timeout: 15000 },
  testDir: "./tests",
  timeout: 60000,
  workers: 1,
  use: {
    baseURL: process.env.E2E_BASE_URL || "http://localhost:3000",
    headless: true,
    actionTimeout: 15000,
    timezoneId: "Asia/Kolkata",
    viewport: { width: 1440, height: 1000 },
  },
  reporter: "list",
});
