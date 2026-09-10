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
export function CategoriesPage() {
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState({ name: "", icon: "🏷️" });
  const [error, setError] = useState("");
  const load = () =>
    api
      .categories()
      .then(setCategories)
      .catch((e) => setError(e.message));
  useEffect(() => {
    load();
  }, []);
  const submit = async (e) => {
    e.preventDefault();
    setError("");
    try {
      const created = await api.createCategory({ ...form });
      setCategories((items) =>
        [...items, created].sort((a, b) => a.name.localeCompare(b.name)),
      );
      setForm({ name: "", icon: "🏷️" });
    } catch (e) {
      setError(e.message);
    }
  };
  return (
    <>
      <PageHeader
        eyebrow="Personalize"
        title="Expense categories"
        description="Use prepared categories or create categories specific to your life."
      />
      <div className="content-split">
        <section className="category-grid">
          {categories.map((category) => (
            <div className="category-card" key={category.id}>
              <span>{category.icon}</span>
              <strong>{category.name}</strong>
              <small>{category.systemDefined ? "Prepared" : "Custom"}</small>
            </div>
          ))}
        </section>
        <form className="panel form-card compact" onSubmit={submit}>
          <h2>Create category</h2>
          <label>
            Name
            <input
              required
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="College expenses"
            />
          </label>
          <label>
            Icon
            <input
              required
              value={form.icon}
              onChange={(e) => setForm({ ...form, icon: e.target.value })}
            />
          </label>
          {error && <div className="inline-error">{error}</div>}
          <button className="primary-btn">Create category</button>
        </form>
      </div>
    </>
  );
}
