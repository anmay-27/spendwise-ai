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
export function AuthPage({ onAuthenticated, theme, toggleTheme }) {
  const [register, setRegister] = useState(false);
  const [form, setForm] = useState({ name: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      onAuthenticated(await (register ? api.register(form) : api.login(form)));
    } catch (exception) {
      setError(exception.message);
    } finally {
      setLoading(false);
    }
  };
  return (
    <main className="auth-page">
      <div className="auth-topbar">
        <div className="auth-brand">
          <div className="brand-mark">S</div>
          <strong>SpendWise</strong>
        </div>
        <ThemeToggle compact theme={theme} toggleTheme={toggleTheme} />
      </div>
      <section className="auth-content">
        <div className="auth-intro">
          <span className="eyebrow">Personal finance, clarified</span>
          <h1>Spend with confidence.</h1>
          <p>
            One considered view of your wallet, budgets, transactions, and
            monthly patterns.
          </p>
        </div>
        <form className="auth-form" onSubmit={submit}>
          <div>
            <span className="auth-kicker">Welcome back</span>
            <h2>Sign in to your account</h2>
          </div>
          {register && (
            <label>
              Name
              <input
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </label>
          )}
          <label>
            Email address
            <input
              required
              type="email"
              autoComplete="email"
              value={form.email}
              onChange={(event) =>
                setForm({ ...form, email: event.target.value })
              }
            />
          </label>
          <label>
            Password
            <input
              required
              type="password"
              autoComplete="current-password"
              value={form.password}
              onChange={(event) =>
                setForm({ ...form, password: event.target.value })
              }
            />
          </label>
          {error && <div className="inline-error">{error}</div>}
          <button className="primary-btn large" disabled={loading}>
            <LogIn size={18} />
            {loading
              ? "Please wait..."
              : register
                ? "Create account"
                : "Sign in"}
          </button>
          <button
            type="button"
            className="text-btn"
            onClick={() => setRegister(!register)}
          >
            {register ? "Already registered? Sign in" : "Create a new account"}
          </button>
          {api.googleLoginEnabled && (
            <a href={api.googleLoginUrl}>Continue with Google</a>
          )}
        </form>
      </section>
    </main>
  );
}
