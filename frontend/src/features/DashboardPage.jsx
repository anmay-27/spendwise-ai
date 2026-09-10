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
export function DashboardPage({ onNavigate }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const load = () =>
    api
      .dashboard()
      .then(setData)
      .catch((e) => setError(e.message));
  useEffect(() => {
    load();
  }, []);
  if (error) return <ErrorCard error={error} />;
  if (!data) return <Loading />;

  return (
    <>
      <PageHeader
        eyebrow="Overview"
        title={`Good to see you, ${data.userName}`}
        description="Your wallet, budgets, and AI-classified spending in one place."
        action={
          <button className="primary-btn" onClick={() => onNavigate("pay")}>
            <Plus size={17} /> Make payment
          </button>
        }
      />
      <section className="stats-grid">
        <StatCard
          label="Demo wallet"
          value={money(data.walletBalance)}
          hint="Available balance"
        />
        <StatCard
          label="Spent this month"
          value={money(data.totalSpentThisMonth)}
          hint={`${data.categorySpending.length} active categories`}
        />
        <StatCard
          label="Remaining budget"
          value={money(data.remainingBudget)}
          hint={`From ${money(data.totalBudget)} configured`}
        />
      </section>
      <section className="dashboard-grid">
        <div className="panel spending-panel">
          <PanelTitle title="Category spending" subtitle="Current month" />
          <SpendingVisual categories={data.categorySpending} />
        </div>
        <div className="panel">
          <PanelTitle
            title="Budget watch"
            subtitle="Projected using this month's pace"
          />
          <div className="budget-list">
            {data.budgets.map((budget) => (
              <BudgetBar key={budget.budgetId} budget={budget} />
            ))}
          </div>
        </div>
      </section>
      <section className="panel">
        <PanelTitle
          title="Recent transactions"
          subtitle="AI suggestion and final category"
          action={
            <button
              className="text-btn"
              onClick={() => onNavigate("transactions")}
            >
              View all →
            </button>
          }
        />
        <TransactionTable transactions={data.recentTransactions} />
      </section>
    </>
  );
}
