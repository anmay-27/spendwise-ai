import { useEffect, useState } from "react";
import { request, api } from "../api";
import {
  PageHeader,
  StatCard,
  PanelTitle,
  ErrorCard,
  Loading,
} from "../components/FinanceUI";
const money = (value) =>
  "₹" +
  Number(value || 0).toLocaleString("en-IN", { maximumFractionDigits: 2 });
const currentMonth = () => new Date().toLocaleDateString("en-CA").slice(0, 7);
import { useData } from "./useFinanceData";
import { Bars } from "../components/TrendBars";
export function FinanceOverview({ full = false }) {
  const [month, setMonth] = useState(currentMonth);
  const { data, error } = useData("/analytics?month=" + month);
  if (error) return <ErrorCard error={error} />;
  if (!data) return <Loading />;
  return (
    <section>
      <PageHeader
        eyebrow="Financial overview"
        title={full ? "Analytics" : "Your money this month"}
        description="Calculated from your recorded income and expenses."
        action={
          <input
            aria-label="Analytics month"
            type="month"
            value={month}
            onChange={(e) => setMonth(e.target.value)}
          />
        }
      />
      <div className="stats-grid finance-stats">
        <StatCard label="Income" value={money(data.income)} hint={month} />
        <StatCard
          label="Expenses"
          value={money(data.expenses)}
          hint="Recorded spending"
        />
        <StatCard
          label="Savings"
          value={money(data.savings)}
          hint={data.savingsRate + "% savings rate"}
        />
        <StatCard
          label="Subscriptions"
          value={money(data.subscriptionTotal)}
          hint="Estimated monthly recurring cost"
        />
      </div>
      <div className="dashboard-grid">
        <div className="panel">
          <PanelTitle
            title="Monthly spending trend"
            subtitle="Six calendar months"
          />
          <Bars values={data.trend} />
        </div>
        <div className="panel">
          <PanelTitle
            title="Personal insights"
            subtitle="Calculated facts, no LLM required"
          />
          {data.insights.map((s, i) => (
            <p className="insight" key={i}>
              {s}
            </p>
          ))}
        </div>
      </div>
      {data.anomalies.length > 0 && (
        <div className="panel">
          <PanelTitle
            title="Unusual spending"
            subtitle="Statistical signals for review"
          />
          {data.anomalies.map((a) => (
            <p key={a.transactionId}>
              {a.merchant}: <strong>{money(a.amount)}</strong> · typical
              category average {money(a.historicalAverage)}
            </p>
          ))}
        </div>
      )}
      {full && (
        <>
          <div className="dashboard-grid">
            <div className="panel">
              <PanelTitle title="Merchant spending" />
              <Bars values={data.merchants} />
            </div>
            <div className="panel">
              <PanelTitle title="Weekly spending" />
              <Bars values={data.weekly} />
            </div>
          </div>
          <div className="stats-grid">
            <StatCard
              label="Previous month"
              value={money(data.previousExpenses)}
              hint={
                data.changePercent === null
                  ? "No comparison baseline"
                  : data.changePercent + "% change"
              }
            />
            <StatCard
              label="Average daily spend"
              value={money(data.averageDailySpend)}
              hint="Calendar-day average"
            />
            <StatCard
              label="Largest transaction"
              value={money(data.largestTransaction)}
              hint="Income or expense"
            />
          </div>
        </>
      )}
    </section>
  );
}
