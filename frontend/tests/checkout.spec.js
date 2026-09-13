import { test, expect } from "@playwright/test";

async function paymentPage(page, configured) {
  // UI contract tests deliberately mock the provider; no real Razorpay transaction is made.
  await page.route("**/api/auth/me", (route) =>
    route.fulfill({
      json: { id: 1, name: "Payment tester", email: "test@example.local" },
    }),
  );
  await page.route("**/api/checkout/config", (route) =>
    route.fulfill({ json: { configured, testMode: true } }),
  );
  await page.route("**/api/categories", (route) =>
    route.fulfill({
      json: [
        { id: 1, name: "Food" },
        { id: 2, name: "Shopping" },
      ],
    }),
  );
  await page.route("**/api/budgets", (route) =>
    route.fulfill({ json: [{ categoryId: 1, limit: 1000, spent: 100 }] }),
  );
  await page
    .context()
    .addCookies([
      {
        name: "XSRF-TOKEN",
        value: "ui-test-csrf",
        url: "http://localhost:3000",
      },
    ]);
  await page.goto("/");
  await page
    .locator("nav")
    .getByRole("button", { name: "Pay", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Make a payment. Stay in control." }),
  ).toBeVisible();
}

test("missing test keys show setup and retain the wallet simulation", async ({
  page,
}) => {
  await page.route("**/api/checkout/orders", (route) =>
    route.fulfill({ json: [] }),
  );
  await paymentPage(page, false);
  await expect(
    page.getByText("Connect Razorpay Test Mode to enable checkout"),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: /Pay .* in Test Mode/ }),
  ).toBeDisabled();
  await page
    .getByRole("button", { name: "Local wallet simulation", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Pay and categorize instantly" }),
  ).toBeVisible();
});

test("checkout callback verifies on server before displaying a captured receipt", async ({
  page,
}) => {
  const order = {
    id: "test-order",
    provider_order_id: "order_test123",
    amount: 150,
    amountPaise: 15000,
    currency: "INR",
    keyId: "rzp_test_example",
    merchant: "Campus Café",
    status: "CREATED",
    created_at: "2026-09-13T12:00:00",
  };
  let created = false,
    verified = false;
  await page.route("**/api/checkout/orders", (route) => {
    if (route.request().method() === "POST") {
      created = true;
      return route.fulfill({ json: order });
    }
    return route.fulfill({ json: created ? [order] : [] });
  });
  await page.route(
    "**/api/checkout/orders/test-order/verify",
    async (route) => {
      expect(route.request().postDataJSON()).toEqual({
        paymentId: "pay_test123",
        signature: "a".repeat(64),
      });
      verified = true;
      order.status = "CAPTURED";
      order.provider_payment_id = "pay_test123";
      order.transaction_id = 99;
      await route.fulfill({ json: order });
    },
  );
  await page.addInitScript(() => {
    window.Razorpay = class {
      constructor(options) {
        this.options = options;
      }
      on() {}
      open() {
        this.options.handler({
          razorpay_payment_id: "pay_test123",
          razorpay_signature: "a".repeat(64),
        });
      }
    };
  });
  await paymentPage(page, true);
  await expect(page.getByText(/750.*would remain/)).toBeVisible();
  await page.getByRole("button", { name: /Pay .* in Test Mode/ }).click();
  await expect(page.getByRole("status")).toContainText(
    "Test payment confirmed",
  );
  expect(verified).toBe(true);
  await expect(
    page.getByRole("region", { name: "Payment receipt" }),
  ).toContainText("pay_test123");
  await expect(
    page.getByRole("region", { name: "Payment receipt" }),
  ).toContainText("#99");
});

test("closing checkout does not display a successful payment", async ({
  page,
}) => {
  const order = {
    id: "test-order",
    provider_order_id: "order_test123",
    amount: 150,
    amountPaise: 15000,
    currency: "INR",
    keyId: "rzp_test_example",
    merchant: "Campus Café",
    status: "CREATED",
  };
  await page.route("**/api/checkout/orders", (route) =>
    route.fulfill({ json: route.request().method() === "POST" ? order : [] }),
  );
  await page.addInitScript(() => {
    window.Razorpay = class {
      constructor(options) {
        this.options = options;
      }
      on() {}
      open() {
        this.options.modal.ondismiss();
      }
    };
  });
  await paymentPage(page, true);
  await page.getByRole("button", { name: /Pay .* in Test Mode/ }).click();
  await expect(page.getByRole("status")).toContainText("Checkout closed");
  await expect(
    page.getByRole("region", { name: "Payment receipt" }),
  ).toContainText("CREATED");
});
