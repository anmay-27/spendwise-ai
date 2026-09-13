import { useEffect, useRef, useState } from "react";
import {
  CreditCard,
  ShieldCheck,
  ArrowUpRight,
  Receipt,
  Coffee,
  ShoppingBag,
  Film,
} from "lucide-react";
import { api, request } from "../api";
import { money } from "../lib/format";
import { PageHeader, ErrorCard } from "../components/FinanceUI";
import { WalletPayPage } from "./WalletPayPage";

let checkoutScript;
function loadCheckout() {
  if (window.Razorpay) return Promise.resolve();
  if (!checkoutScript)
    checkoutScript = new Promise((resolve, reject) => {
      const script = document.createElement("script");
      script.src = "https://checkout.razorpay.com/v1/checkout.js";
      script.onload = () =>
        window.Razorpay
          ? resolve()
          : reject(new Error("Checkout could not initialize"));
      script.onerror = () => {
        script.remove();
        reject(
          new Error("Could not load Razorpay. Check your internet connection."),
        );
      };
      document.head.appendChild(script);
    }).catch((error) => {
      checkoutScript = null;
      throw error;
    });
  return checkoutScript;
}
const merchants = [
  { name: "Campus Café", category: "Food", icon: Coffee },
  { name: "Everyday Store", category: "Shopping", icon: ShoppingBag },
  { name: "Movie Night", category: "Entertainment", icon: Film },
];
export function RazorpayPayPage({ onDone }) {
  const [mode, setMode] = useState("razorpay"),
    [config, setConfig] = useState(null);
  const [categories, setCategories] = useState([]),
    [budgets, setBudgets] = useState([]),
    [orders, setOrders] = useState([]);
  const [form, setForm] = useState({
    merchant: "Campus Café",
    amount: "150",
    categoryId: "",
    notes: "",
  });
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [message, setMessage] = useState("");
  const [receipt, setReceipt] = useState(null);
  const pending = useRef(null);
  const refresh = async () => setOrders(await request("/checkout/orders"));
  useEffect(() => {
    let active = true;
    Promise.all([
      request("/checkout/config"),
      api.categories(),
      api.budgets(),
      request("/checkout/orders"),
    ])
      .then(([config, cats, budgets, orders]) => {
        if (!active) return;
        setConfig(config);
        setCategories(cats);
        setBudgets(budgets);
        setOrders(orders);
        setForm((f) => ({
          ...f,
          categoryId: String(
            cats.find((c) => c.name === "Food")?.id || cats[0]?.id || "",
          ),
        }));
      })
      .catch((e) => active && setError(e.message));
    return () => {
      active = false;
    };
  }, []);
  const showResult = async (result) => {
    setReceipt(result);
    setMessage(
      result.status === "CAPTURED"
        ? "Test payment confirmed. Your expense has been recorded."
        : "Payment is not captured yet. Use Check status to verify it again.",
    );
    if (result.status === "CAPTURED") pending.current = null;
    await refresh();
    setBudgets(await api.budgets());
  };
  const check = async (id) => {
    setBusy(true);
    setError("");
    try {
      await showResult(
        await request(`/checkout/orders/${id}/sync`, { method: "POST" }),
      );
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };
  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await loadCheckout();
      const payload = {
        ...form,
        amount: Number(form.amount),
        categoryId: Number(form.categoryId),
      };
      const fingerprint = JSON.stringify(payload);
      if (pending.current?.fingerprint !== fingerprint)
        pending.current = { fingerprint, key: crypto.randomUUID() };
      const order = await request("/checkout/orders", {
        method: "POST",
        headers: { "Idempotency-Key": pending.current.key },
        body: JSON.stringify(payload),
      });
      setReceipt(order);
      await refresh();
      if (order.status === "CAPTURED") {
        await showResult(order);
        setBusy(false);
        return;
      }
      const checkout = new window.Razorpay({
        key: order.keyId,
        amount: order.amountPaise,
        currency: order.currency,
        order_id: order.provider_order_id,
        name: "SpendWise Test Payments",
        description: `Demo purchase: ${order.merchant}`,
        theme: { color: "#c51c36" },
        handler: async (response) => {
          try {
            await showResult(
              await request(`/checkout/orders/${order.id}/verify`, {
                method: "POST",
                body: JSON.stringify({
                  paymentId: response.razorpay_payment_id,
                  signature: response.razorpay_signature,
                }),
              }),
            );
          } catch (e) {
            setError(
              `${e.message} Use Check status on this receipt before starting another payment.`,
            );
          } finally {
            setBusy(false);
          }
        },
        modal: {
          ondismiss: () => {
            setBusy(false);
            setMessage(
              "Checkout closed. No success was assumed; check the order status if you attempted payment.",
            );
          },
        },
      });
      checkout.on("payment.failed", () => {
        setMessage(
          "The payment attempt failed. You can retry in checkout or close it and check the order status.",
        );
      });
      checkout.open();
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  };
  const budget = budgets.find((b) => String(b.categoryId) === form.categoryId);
  const remaining = budget
    ? Number(budget.limit) - Number(budget.spent) - Number(form.amount || 0)
    : null;
  return (
    <>
      <div className="payment-mode" role="group" aria-label="Payment mode">
        <button
          className={mode === "razorpay" ? "active" : ""}
          disabled={busy}
          onClick={() => setMode("razorpay")}
        >
          Razorpay test checkout
        </button>
        <button
          className={mode === "wallet" ? "active" : ""}
          disabled={busy}
          onClick={() => setMode("wallet")}
        >
          Local wallet simulation
        </button>
      </div>
      {mode === "wallet" ? (
        <WalletPayPage onDone={onDone} />
      ) : (
        <>
          <PageHeader
            eyebrow="PAY • TRACK • UNDERSTAND"
            title="Make a payment. Stay in control."
            description="A provider checkout experience with spending tracked automatically after confirmation."
          />
          <div className="payment-test-banner">
            <ShieldCheck size={20} />
            <div>
              <strong>Test Mode — no real money moves</strong>
              <p>
                These are demo merchants. Checkout uses your configured Razorpay
                test account and does not send money to these businesses.
              </p>
            </div>
          </div>
          {error && <ErrorCard error={error} />}
          {config && !config.configured && (
            <div className="panel payment-setup">
              <strong>Connect Razorpay Test Mode to enable checkout</strong>
              <p>
                Add your test key ID and secret to the backend environment, then
                restart it. Your local wallet simulation is available in the tab
                above.
              </p>
              <a
                href="https://dashboard.razorpay.com/"
                target="_blank"
                rel="noreferrer"
              >
                Open Razorpay Dashboard ↗
              </a>
            </div>
          )}
          <div className="payment-layout">
            <form className="panel form-card" onSubmit={submit}>
              <div className="panel-title">
                <h2>Who are you paying?</h2>
                <CreditCard />
              </div>
              <div className="merchant-shortcuts">
                {merchants.map(({ name, category, icon: Icon }) => (
                  <button
                    key={name}
                    type="button"
                    disabled={busy}
                    className={form.merchant === name ? "selected" : ""}
                    onClick={() =>
                      setForm({
                        ...form,
                        merchant: name,
                        categoryId: String(
                          categories.find((c) => c.name === category)?.id ||
                            form.categoryId,
                        ),
                      })
                    }
                  >
                    <Icon size={21} />
                    <span>{name}</span>
                  </button>
                ))}
              </div>
              <label>
                Demo merchant
                <input
                  required
                  disabled={busy}
                  maxLength={160}
                  value={form.merchant}
                  onChange={(e) =>
                    setForm({ ...form, merchant: e.target.value })
                  }
                />
              </label>
              <label>
                Payment amount (INR)
                <input
                  className="payment-amount"
                  required
                  disabled={busy}
                  type="number"
                  min="1"
                  max="100000"
                  step="0.01"
                  value={form.amount}
                  onChange={(e) => setForm({ ...form, amount: e.target.value })}
                />
              </label>
              <label>
                Expense category
                <select
                  aria-label="Expense category"
                  disabled={busy}
                  required
                  value={form.categoryId}
                  onChange={(e) =>
                    setForm({ ...form, categoryId: e.target.value })
                  }
                >
                  <option value="">Choose category</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Payment note
                <input
                  disabled={busy}
                  maxLength={500}
                  placeholder="What is this payment for?"
                  value={form.notes}
                  onChange={(e) => setForm({ ...form, notes: e.target.value })}
                />
              </label>
              {remaining !== null && (
                <div className="payment-budget">
                  {remaining >= 0
                    ? `${money(remaining)} would remain in this category budget.`
                    : `This payment would exceed this category budget by ${money(-remaining)}.`}
                </div>
              )}
              <button
                className="primary-btn large"
                disabled={busy || !config?.configured}
              >
                {busy
                  ? "Payment in progress…"
                  : `Pay ${money(form.amount)} in Test Mode`}
                <ArrowUpRight size={18} />
              </button>
              <small>
                Confirmed test expenses update your finance dashboard. Your
                simulated wallet balance is separate.
              </small>
            </form>
            <section
              className="panel payment-receipt"
              aria-label="Payment receipt"
            >
              <Receipt size={30} />
              <span className="payment-badge">TEST RECEIPT</span>
              {receipt ? (
                <>
                  <h2>{money(receipt.amount)}</h2>
                  <p>{receipt.merchant}</p>
                  <strong
                    className={`payment-status ${receipt.status.toLowerCase()}`}
                  >
                    {receipt.status}
                  </strong>
                  <dl>
                    <dt>Order reference</dt>
                    <dd>{receipt.provider_order_id}</dd>
                    <dt>Payment reference</dt>
                    <dd>{receipt.provider_payment_id || "Awaiting payment"}</dd>
                    <dt>Expense record</dt>
                    <dd>
                      {receipt.transaction_id
                        ? `#${receipt.transaction_id}`
                        : "Created only after capture"}
                    </dd>
                  </dl>
                  {receipt.status !== "CAPTURED" && (
                    <button
                      className="secondary-btn"
                      disabled={busy || !config?.configured}
                      onClick={() => check(receipt.id)}
                    >
                      Check status
                    </button>
                  )}
                </>
              ) : (
                <>
                  <h2>Your next payment, clearly tracked.</h2>
                  <p>
                    Choose a merchant and amount. Complete test checkout, then
                    see your verified receipt here.
                  </p>
                  <ol>
                    <li>Open secure provider checkout</li>
                    <li>Complete a test payment</li>
                    <li>See your expense and budget update</li>
                  </ol>
                </>
              )}
              {message && (
                <p role="status" className="payment-message">
                  {message}
                </p>
              )}
            </section>
          </div>
          <section className="panel payment-history">
            <div className="panel-title">
              <h2>Recent test payments</h2>
              <button
                onClick={() => refresh().catch((e) => setError(e.message))}
              >
                Refresh history
              </button>
            </div>
            {orders.length === 0 ? (
              <p>
                Your payment history will appear here. Manual expenses and
                wallet simulations stay in Transactions.
              </p>
            ) : (
              orders.map((order) => (
                <div className="payment-history-row" key={order.id}>
                  <div>
                    <strong>{order.merchant}</strong>
                    <small>
                      {order.created_at?.slice(0, 16).replace("T", " ")}
                    </small>
                  </div>
                  <strong>{money(order.amount)}</strong>
                  <span
                    className={`payment-status ${order.status.toLowerCase()}`}
                  >
                    {order.status}
                  </span>
                  <button
                    onClick={() => {
                      setReceipt(order);
                      setMessage("");
                    }}
                  >
                    Receipt
                  </button>
                  {order.status !== "CAPTURED" && (
                    <button
                      disabled={busy || !config?.configured}
                      onClick={() => check(order.id)}
                    >
                      Check status
                    </button>
                  )}
                </div>
              ))
            )}
          </section>
        </>
      )}
    </>
  );
}
