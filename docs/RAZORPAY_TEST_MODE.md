# Razorpay Test Mode

SpendWise now offers Razorpay Standard Checkout from **Pay**, alongside the previous local wallet simulation. Demo merchant names are labels for test purchases on your Razorpay account; they do not route money to arbitrary businesses or UPI IDs. Live keys are rejected at backend startup.

## Configure your account

1. Open https://dashboard.razorpay.com/, switch to Test Mode, and generate a test key ID and secret.
2. Add these entries to the ignored root `.env` file:

```dotenv
RAZORPAY_KEY_ID=rzp_test_your_key
RAZORPAY_KEY_SECRET=your_test_secret
RAZORPAY_WEBHOOK_SECRET=your_separate_random_webhook_secret
```

3. Start Docker Desktop, then run `docker compose up --build -d` from the project root.
4. Open http://localhost:3000, sign in, and select **Pay → Razorpay test checkout**.
5. Choose a demo merchant/category, enter INR 1–100,000 and open checkout. Use Razorpay's documented test payment methods. No real funds move.
6. On success, the backend verifies the checkout signature using the stored order ID, fetches the payment from Razorpay and checks its order, amount, currency and status. Only `captured` creates an expense.

Use **Check status** for an authorized/pending payment, after a network interruption, or after closing checkout. Configure automatic payment capture in your Razorpay test account. Authorization alone is not displayed as confirmed spending.

## Webhook setup

The endpoint is `POST /api/checkout/webhook`. Expose it through a publicly reachable HTTPS backend or development tunnel to the Gateway port (18080). Do not expose database or management ports. In Razorpay's Test Mode webhook settings, enter the public URL ending in `/api/checkout/webhook` and the same separate webhook secret.

Subscribe to `payment.captured`, `payment.authorized`, `payment.failed` and `order.paid`. The backend verifies `X-Razorpay-Signature` against the exact raw body and fetches current payment state before applying it. This endpoint alone is exempt from browser CSRF; authenticated order endpoints retain CSRF and ownership checks. Webhooks work without a logged-in browser.

Without a public webhook, checkout verification and **Check status** still work. Background recovery when the browser is closed requires webhook setup. Webhook/API outages can delay confirmation; retries and status checks are safe.

## Data and reliability

- Flyway V4 adds `checkout_orders` and a provider-payment flag on expenses.
- Requests use owner-scoped idempotency keys. The UI reuses the same key when retrying the same form.
- Row locks serialize callbacks, webhooks and status checks. A captured order produces one expense and one payment notification event, committed atomically with the order update.
- Provider-created expenses can be recategorized, but their amount cannot be edited and they cannot be deleted through ledger CRUD.
- Test purchases are included in the demo finance totals, but never debit or credit the simulated wallet.
- Only the public key ID is returned to checkout. API and webhook secrets stay in the backend environment.
- The test amount is deliberately user-entered. This endpoint must not be reused to sell priced products without a server-owned product/price catalog.

## Verification and limits

Java integration tests use PostgreSQL and mocked provider responses for idempotency, ownership, capture, pending/failure states and mismatched amounts. Separate signature tests use real HMAC. Browser contract tests stub checkout and test setup, success and dismissal screens. These automated tests are not evidence of a completed transaction in Razorpay's actual sandbox.

Actual sandbox checkout requires your test credentials. Refund initiation/reconciliation, payouts, arbitrary UPI transfers, recurring billing and live payments are not implemented. Do not issue refunds through the Razorpay dashboard and assume SpendWise will automatically reverse the expense in this version. Payment history currently shows the most recent 50 orders. A provider-order request timeout can leave an unpaid orphan order in Razorpay; it cannot itself charge money because checkout has not opened.

References: [Standard Checkout](https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/integration-steps/), [webhooks](https://razorpay.com/docs/webhooks/), [Test Mode](https://razorpay.com/docs/payments/dashboard/test-live-modes/).
