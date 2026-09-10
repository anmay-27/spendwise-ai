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
export function ReportsPage() {
  const [notice, setNotice] = useState("");
  const [report, setReport] = useState(null);
  const [error, setError] = useState("");
  useEffect(() => {
    api
      .report()
      .then(setReport)
      .catch((e) => setError(e.message));
  }, []);
  if (error) return <ErrorCard error={error} />;
  if (!report) return <Loading />;
  return (
    <>
      <PageHeader
        eyebrow="Monthly intelligence"
        title="Spending report"
        description="A database-calculated report with readable insights."
      />
      <div className="panel">
        <button
          className="primary-btn"
          onClick={async () => {
            try {
              await request("/reports/generate", { method: "POST" });
              setNotice(
                "Report generated. A notification will arrive shortly.",
              );
            } catch (e) {
              setNotice(e.message);
            }
          }}
        >
          Generate report notification
        </button>{" "}
        <button className="secondary-btn" onClick={() => window.print()}>
          Print / Save PDF
        </button>
        <p>{notice}</p>
      </div>
      <section className="stats-grid">
        <StatCard
          label="Total spent"
          value={money(report.totalSpent)}
          hint={`${report.changePercent >= 0 ? "+" : ""}${report.changePercent}% vs previous month`}
        />
        <StatCard
          label="Previous month"
          value={money(report.previousMonthSpent)}
          hint="Comparison baseline"
        />
        <StatCard
          label="Highest category"
          value={report.highestCategory}
          hint={money(report.highestCategoryAmount)}
        />
      </section>
      <section className="dashboard-grid">
        <div className="panel">
          <PanelTitle
            title="Breakdown"
            subtitle={`${report.month}/${report.year}`}
          />
          <div className="report-bars">
            {report.categorySpending.map((item) => (
              <div key={item.category}>
                <div>
                  <span>
                    {item.icon} {item.category}
                  </span>
                  <strong>{money(item.amount)}</strong>
                </div>
                <div className="report-track">
                  <span style={{ width: `${item.percentage}%` }} />
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="panel insight-panel">
          <PanelTitle
            title="Smart insights"
            subtitle="Generated from your actual data"
          />
          {report.insights.map((insight) => (
            <div className="insight" key={insight}>
              ✦ <span>{insight}</span>
            </div>
          ))}
        </div>
      </section>
    </>
  );
}
