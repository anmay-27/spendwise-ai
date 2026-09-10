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
export function BudgetsPage() {
  const [budgets, setBudgets] = useState([]);
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState({
    categoryId: "",
    monthlyLimit: "",
    warningPercent: 80,
    month: new Date().toISOString().slice(0, 7),
  });
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const load = () =>
    Promise.all([api.budgets(form.month), api.categories()])
      .then(([b, c]) => {
        setBudgets(b);
        setCategories(c);
        if (!form.categoryId && c[0])
          setForm((f) => ({ ...f, categoryId: c[0].id }));
      })
      .catch((e) => setError(e.message));
  useEffect(() => {
    load();
  }, [form.month]);
  const submit = async (e) => {
    e.preventDefault();
    setError("");
    try {
      setBudgets(
        await api.setBudget({
          categoryId: Number(form.categoryId),
          monthlyLimit: Number(form.monthlyLimit),
          warningPercent: Number(form.warningPercent),
          month: form.month,
        }),
      );
      setMessage("Budget saved.");
      setForm((f) => ({ ...f, monthlyLimit: "" }));
    } catch (e) {
      setError(e.message);
    }
  };
  return (
    <>
      <PageHeader
        eyebrow="Controls"
        title="Monthly budgets"
        description="Limits are optional. Warnings appear when a category reaches your chosen percentage."
      />
      <div className="content-split">
        <section className="panel">
          <PanelTitle
            title="Current limits"
            subtitle="Live spend and projected month-end value"
          />
          <div className="budget-list">
            {budgets.map((b) => (
              <div key={b.budgetId}>
                <BudgetBar budget={b} />
                <button
                  className="text-btn"
                  onClick={async () => {
                    try {
                      await request("/budgets/" + b.budgetId, {
                        method: "DELETE",
                      });
                      load();
                    } catch (e) {
                      setError(e.message);
                    }
                  }}
                >
                  Remove budget
                </button>
              </div>
            ))}
          </div>
        </section>
        <form className="panel form-card compact" onSubmit={submit}>
          <h2>Set or update budget</h2>
          <label>
            Category
            <select
              value={form.categoryId}
              onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
            >
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.icon} {c.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Budget month
            <input
              type="month"
              required
              value={form.month}
              onChange={(e) => setForm({ ...form, month: e.target.value })}
            />
          </label>
          <label>
            Monthly limit
            <input
              required
              type="number"
              min="1"
              value={form.monthlyLimit}
              onChange={(e) =>
                setForm({ ...form, monthlyLimit: e.target.value })
              }
              placeholder="5000"
            />
          </label>
          <label>
            Warn at
            <input
              required
              type="number"
              min="1"
              max="100"
              value={form.warningPercent}
              onChange={(e) =>
                setForm({ ...form, warningPercent: e.target.value })
              }
            />
          </label>
          {message && <div className="success-note">{message}</div>}
          {error && <div className="inline-error">{error}</div>}
          <button className="primary-btn">Save budget</button>
        </form>
      </div>
    </>
  );
}
