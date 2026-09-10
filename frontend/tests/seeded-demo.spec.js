import { test, expect } from "@playwright/test";
import { readFileSync, existsSync } from "node:fs";

test("seeded dashboard, populated subscriptions and insights render without errors", async ({ page }) => {
  const envPath = new URL("../../.env", import.meta.url);
  test.skip(!existsSync(envPath), "Requires the opt-in Compose demo configuration");
  const env = Object.fromEntries(readFileSync(envPath, "utf8").split(/\r?\n/)
    .filter(line => line.includes("=")).map(line => [line.slice(0, line.indexOf("=")), line.slice(line.indexOf("=") + 1)]));
  test.skip(env.DEMO_ENABLED !== "true", "Demo seeding is not enabled");
  const errors = [];
  page.on("pageerror", error => errors.push(error.message));
  await page.goto("/");
  await page.getByLabel("Email address").fill("demo@spendwise.local");
  await page.getByLabel("Password").fill(env.DEMO_PASSWORD);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Your money this month" })).toBeVisible();
  await expect(page.getByText(/Loading your money data/)).toHaveCount(0);
  await expect(page.locator(".state-card.error")).toHaveCount(0);
  await page.screenshot({ path: "test-results/demo-dashboard.png", fullPage: true });
  for (const name of ["Subscriptions", "AI Insights", "Analytics", "Savings Goals", "Reports"]) {
    await page.locator("nav").getByRole("button", { name, exact: true }).click();
    await expect(page.getByText(/Loading your money data/)).toHaveCount(0);
    await expect(page.locator(".state-card.error")).toHaveCount(0);
    if (name === "Subscriptions" || name === "AI Insights")
      await expect(page.locator(".insight").first()).toBeVisible();
  }
  expect(errors).toEqual([]);
});
