import { useEffect, useMemo, useState, useRef } from "react";
import {
  ArrowLeftRight,
  Bot,
  ChartNoAxesCombined,
  CircleDollarSign,
  CreditCard,
  LayoutDashboard,
  LogIn,
  LogOut,
  Moon,
  Plus,
  Shapes,
  Sun,
} from "lucide-react";
import { api, request } from "../api";
import { money, date } from "../lib/format";
import {
  ThemeToggle,
  PageHeader,
  Loading,
  ErrorCard,
  StatCard,
  PanelTitle,
  SpendingVisual,
  BudgetBar,
  TransactionTable,
} from "../components/FinanceUI";
export function PayPage({ onDone }) {
  const pendingPayment = useRef(null);
  const [form, setForm] = useState({
    merchant: "",
    amount: "",
    description: "",
  });
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    setResult(null);
    try {
      const payload = {
        merchant: form.merchant,
        amount: Number(form.amount),
        description: form.description,
      };
      const fingerprint = JSON.stringify(payload);
      if (pendingPayment.current?.fingerprint !== fingerprint)
        pendingPayment.current = { fingerprint, key: crypto.randomUUID() };
      setResult(await api.pay(payload, pendingPayment.current.key));
      pendingPayment.current = null;
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };
  return (
    <>
      <PageHeader
        eyebrow="Demo payment"
        title="Pay and categorize instantly"
        description="This simulates a wallet payment. No real money is transferred."
      />
      <div className="two-column-form">
        <form className="panel form-card" onSubmit={submit}>
          <label>
            Merchant name
            <input
              required
              value={form.merchant}
              onChange={(e) => setForm({ ...form, merchant: e.target.value })}
              placeholder="PVR Cinemas"
            />
          </label>
          <label>
            Amount
            <input
              required
              min="1"
              step="0.01"
              type="number"
              value={form.amount}
              onChange={(e) => setForm({ ...form, amount: e.target.value })}
              placeholder="650"
            />
          </label>
          <label>
            Description
            <textarea
              value={form.description}
              onChange={(e) =>
                setForm({ ...form, description: e.target.value })
              }
              placeholder="Movie tickets"
            />
          </label>
          {error && <div className="inline-error">{error}</div>}
          <button className="primary-btn large" disabled={loading}>
            {loading
              ? "Predicting category…"
              : `Pay ${form.amount ? money(form.amount) : ""}`}
          </button>
        </form>
        <div className="panel result-card">
          {!result ? (
            <div className="result-placeholder">
              <div>✦</div>
              <h2>AI category preview</h2>
              <p>
                The ML service uses merchant and description text, then the
                backend checks your category budget.
              </p>
            </div>
          ) : (
            <div className="success-result">
              <div className="success-mark">✓</div>
              <span>Payment successful</span>
              <h2>
                {money(result.amount)} to {result.merchant}
              </h2>
              <div className="prediction">
                <span>AI category</span>
                <strong>
                  {result.categoryIcon} {result.category}
                </strong>
                <small>{Math.round(result.confidence * 100)}% confidence</small>
              </div>
              {result.budgetWarning && (
                <div className="warning-box">⚠ {result.budgetWarning}</div>
              )}
              <p>
                Wallet balance: <strong>{money(result.walletBalance)}</strong>
              </p>
              <button className="secondary-btn" onClick={onDone}>
                Back to dashboard
              </button>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
