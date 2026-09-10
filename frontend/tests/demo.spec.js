import { test, expect } from "@playwright/test";
test("register, navigate finance screens, create transaction and receive a Kafka report notification", async ({
  page,
}) => {
  test.setTimeout(180000);
  await expect
    .poll(
      async () => {
        try {
          return (await page.request.get("/")).status();
        } catch {
          return 0;
        }
      },
      { timeout: 120000 },
    )
    .toBe(200);
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await expect(page.getByRole("button", { name: "Create a new account" })).toBeVisible({ timeout: 45000 });
  await page.getByRole("button", { name: "Create a new account" }).click();
  await page.getByLabel("Name", { exact: true }).fill("Browser Test");
  await page
    .getByLabel("Email address")
    .fill("browser-" + Date.now() + "@test.local");
  await page.getByLabel("Password").fill("BrowserTestPassword123!");
  await page
    .getByRole("button", { name: "Create account", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Your money this month" }),
  ).toBeVisible();
  await page
    .locator("nav")
    .getByRole("button", { name: "Transactions", exact: true })
    .click();
  await page.getByLabel("Merchant", { exact: true }).fill("Browser groceries");
  await page.getByLabel("Amount", { exact: true }).fill("850");
  await page
    .getByRole("combobox", { name: "Category", exact: true })
    .selectOption({ label: "Food" });
  await page
    .getByRole("button", { name: "Add transaction", exact: true })
    .click();
  await expect(
    page.locator(".ledger-row").filter({ hasText: "Browser groceries" }),
  ).toBeVisible();
  await page
    .locator("nav")
    .getByRole("button", { name: "AI Chat", exact: true })
    .click();
  await page
    .getByPlaceholder("Ask about your spending…")
    .fill("How much did I spend on food this month?");
  await page.getByRole("button", { name: "Ask", exact: true }).click();
  await expect(page.locator(".message.assistant").last()).toContainText("850");
  for (const name of [
    "Budgets",
    "Savings Goals",
    "Subscriptions",
    "AI Insights",
    "Analytics",
  ]) {
    await page
      .locator("nav")
      .getByRole("button", { name, exact: true })
      .click();
    await expect(page.getByText(/Loading your money data/)).toHaveCount(0);
    await expect(page.locator(".state-card.error")).toHaveCount(0);
  }
  await page
    .locator("nav")
    .getByRole("button", { name: "Reports", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Generate report notification" })
    .click();
  await expect(
    page.getByText("Report generated. A notification will arrive shortly."),
  ).toBeVisible();
  await page
    .locator("nav")
    .getByRole("button", { name: "Notifications", exact: true })
    .click();
  await expect(page.getByText("Your monthly report is ready")).toBeVisible({
    timeout: 45000,
  });
  await page.getByRole("button", { name: "Mark read" }).first().click();
  await expect(page.getByRole("button", { name: "Mark read" })).toHaveCount(0);
  expect(errors).toEqual([]);
});
