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
export function DataPage({ kind }) {
  const { data, error, load } = useData("/" + kind);
  const [failure, setFailure] = useState("");
  useEffect(() => {
    if (kind !== "notifications") return;
    const timer = setInterval(load, 15000);
    return () => clearInterval(timer);
  }, [kind]);
  const titles = {
    subscriptions: "Subscriptions",
    insights: "AI Insights",
    notifications: "Notifications",
  };
  return (
    <>
      <PageHeader
        eyebrow="SpendWise intelligence"
        title={titles[kind]}
        description={
          kind === "notifications"
            ? "Budget, spending, subscription, goal and report updates. Refreshes every 15 seconds."
            : "Based on your recorded transactions."
        }
      />
      {(error || failure) && <ErrorCard error={error || failure} />}
      <div className="panel">
        {!data ? (
          <Loading />
        ) : data.length === 0 ? (
          <p>No {kind} yet.</p>
        ) : (
          data.map((item, i) => (
            <div className="insight" key={item.event_id || i}>
              {kind === "insights" ? (
                item
              ) : kind === "subscriptions" ? (
                <div>
                  <strong>{item.merchant}</strong>
                  <p>
                    {money(item.monthlyCost)}/month · {money(item.annualCost)}
                    /year
                  </p>
                  <small>Next expected payment: {item.nextPayment}</small>
                </div>
              ) : (
                <div>
                  <strong>{item.message}</strong>
                  <p>{item.created_at}</p>
                  {!item.is_read && (
                    <button
                      onClick={async () => {
                        try {
                          await request(
                            "/notifications/" + item.event_id + "/read",
                            { method: "PATCH" },
                          );
                          load();
                        } catch (e) {
                          setFailure(e.message);
                        }
                      }}
                    >
                      Mark read
                    </button>
                  )}
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </>
  );
}
