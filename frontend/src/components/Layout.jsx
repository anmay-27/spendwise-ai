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
const NAV_ITEMS = [
  ["dashboard", LayoutDashboard, "Dashboard"],
  ["pay", CreditCard, "Pay"],
  ["transactions", ArrowLeftRight, "Transactions"],
  ["budgets", CircleDollarSign, "Budgets"],
  ["categories", Shapes, "Categories"],
  ["reports", ChartNoAxesCombined, "Reports"],
  ["assistant", Bot, "AI Chat"],
  ["analytics", ChartNoAxesCombined, "Analytics"],
  ["goals", CircleDollarSign, "Savings Goals"],
  ["subscriptions", CreditCard, "Subscriptions"],
  ["insights", Bot, "AI Insights"],
  ["notifications", Shapes, "Notifications"],
];

export function Sidebar({ page, setPage, user, logout, theme, toggleTheme }) {
  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark">S</div>
        <div>
          <strong>SpendWise</strong>
          <span>AI money manager</span>
        </div>
      </div>
      <nav>
        {NAV_ITEMS.map(([id, Icon, label]) => (
          <button
            key={id}
            className={page === id ? "nav-item active" : "nav-item"}
            onClick={() => setPage(id)}
          >
            <Icon size={19} strokeWidth={1.8} />
            <span>{label}</span>
          </button>
        ))}
      </nav>
      <div className="sidebar-footer">
        <ThemeToggle theme={theme} toggleTheme={toggleTheme} />
        <div className="demo-user">
          <div className="avatar">{user.name?.[0]?.toUpperCase() || "U"}</div>
          <div>
            <strong>{user.name}</strong>
            <span>{user.email}</span>
          </div>
          <button
            className="logout-btn"
            onClick={logout}
            aria-label="Sign out"
            title="Sign out"
          >
            <LogOut size={17} />
          </button>
        </div>
      </div>
    </aside>
  );
}
export function MobileHeader({ page, theme, toggleTheme }) {
  const label = NAV_ITEMS.find(([id]) => id === page)?.[2] || "SpendWise";
  return (
    <div className="mobile-header">
      <div>
        <strong>SpendWise</strong>
        <span>{label}</span>
      </div>
      <ThemeToggle compact theme={theme} toggleTheme={toggleTheme} />
    </div>
  );
}
