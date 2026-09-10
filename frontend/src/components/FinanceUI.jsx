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
export function ThemeToggle({ theme, toggleTheme, compact = false }) {
  const Icon = theme === "dark" ? Sun : Moon;
  const label =
    theme === "dark" ? "Switch to light mode" : "Switch to dark mode";
  return (
    <button
      className={compact ? "theme-toggle compact" : "theme-toggle"}
      onClick={toggleTheme}
      aria-label={label}
      title={label}
    >
      <Icon size={18} strokeWidth={1.8} />
      {!compact && <span>{theme === "dark" ? "Light mode" : "Dark mode"}</span>}
    </button>
  );
}

export function PageHeader({ eyebrow, title, description, action }) {
  return (
    <header className="page-header">
      <div>
        <span className="eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {action}
    </header>
  );
}

export function Loading() {
  return <div className="state-card">Loading your money data…</div>;
}

export function ErrorCard({ error }) {
  return <div className="state-card error">{error}</div>;
}

export function StatCard({ label, value, hint }) {
  return (
    <div className="stat-card">
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{hint}</small>
    </div>
  );
}

export function PanelTitle({ title, subtitle, action }) {
  return (
    <div className="panel-title">
      <div>
        <h2>{title}</h2>
        <p>{subtitle}</p>
      </div>
      {action}
    </div>
  );
}

export function SpendingVisual({ categories }) {
  const segments = useMemo(() => {
    let cursor = 0;
    const palette = [
      "#c52132",
      "#16815a",
      "#d98b28",
      "#b74466",
      "#417c93",
      "#78656b",
      "#ed6572",
    ];
    const parts = categories.map((item, index) => {
      const start = cursor;
      cursor += item.percentage;
      return `${palette[index % palette.length]} ${start}% ${cursor}%`;
    });
    return parts.length ? `conic-gradient(${parts.join(",")})` : "#e5e7eb";
  }, [categories]);
  return (
    <div className="spending-visual">
      <div className="donut" style={{ background: segments }}>
        <div>
          <strong>{categories.length}</strong>
          <span>categories</span>
        </div>
      </div>
      <div className="legend">
        {categories.slice(0, 6).map((item) => (
          <div key={item.category}>
            <span>
              {item.icon} {item.category}
            </span>
            <strong>{money(item.amount)}</strong>
          </div>
        ))}
      </div>
    </div>
  );
}

export function BudgetBar({ budget }) {
  const width = Math.min(budget.usedPercent, 100);
  return (
    <div className={budget.warning ? "budget-row warning-row" : "budget-row"}>
      <div className="budget-heading">
        <span>
          {budget.icon} {budget.category}
        </span>
        <strong>
          {money(budget.spent)} / {money(budget.limit)}
        </strong>
      </div>
      <div className="progress-track">
        <div style={{ width: `${width}%` }} />
      </div>
      <div className="budget-meta">
        <span>{budget.usedPercent}% used</span>
        <span>Projected {money(budget.projectedSpend)}</span>
      </div>
    </div>
  );
}

export function TransactionTable({
  transactions,
  categories,
  onCategoryChange,
}) {
  if (!transactions.length)
    return <div className="empty">No transactions yet.</div>;
  return (
    <div className="transaction-list">
      {transactions.map((tx) => (
        <div className="transaction-row" key={tx.id}>
          <div className="transaction-icon">{tx.categoryIcon}</div>
          <div className="transaction-main">
            <strong>{tx.merchant}</strong>
            <span>
              {date(tx.occurredAt)} · AI: {tx.aiSuggestedCategory} (
              {Math.round(tx.aiConfidence * 100)}%)
            </span>
          </div>
          {categories ? (
            <select
              value={tx.categoryId}
              onChange={(e) => onCategoryChange(tx.id, Number(e.target.value))}
            >
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          ) : (
            <span className="category-pill">{tx.category}</span>
          )}
          <strong className="amount">
            {tx.type === "INCOME" ? "+" : "−"}
            {money(tx.amount)}
          </strong>
        </div>
      ))}
    </div>
  );
}
