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
export function GoalsPage() {
  const { data, error, load } = useData("/goals");
  const [failure, setFailure] = useState(""),
    [editing, setEditing] = useState(null);
  const empty = {
    name: "",
    targetAmount: "",
    currentAmount: 0,
    targetDate: "",
    version: 0,
  };
  const [form, setForm] = useState(empty);
  const save = async (e) => {
    e.preventDefault();
    try {
      await request("/goals" + (editing ? "/" + editing : ""), {
        method: editing ? "PUT" : "POST",
        body: JSON.stringify({
          ...form,
          targetAmount: Number(form.targetAmount),
          currentAmount: Number(form.currentAmount),
        }),
      });
      setForm(empty);
      setEditing(null);
      load();
    } catch (e) {
      setFailure(e.message);
    }
  };
  return (
    <>
      <PageHeader
        eyebrow="Plan ahead"
        title="Savings goals"
        description="Set a target and compare the required pace with your previous month's savings."
      />
      {(error || failure) && <ErrorCard error={error || failure} />}
      <form className="panel ledger-form" onSubmit={save}>
        <label>
          Goal
          <input
            required
            maxLength={100}
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
        </label>
        <label>
          Target amount
          <input
            required
            type="number"
            min=".01"
            step=".01"
            value={form.targetAmount}
            onChange={(e) => setForm({ ...form, targetAmount: e.target.value })}
          />
        </label>
        <label>
          Saved amount
          <input
            required
            type="number"
            min="0"
            step=".01"
            value={form.currentAmount}
            onChange={(e) =>
              setForm({ ...form, currentAmount: e.target.value })
            }
          />
        </label>
        <label>
          Target date
          <input
            required
            type="date"
            value={form.targetDate}
            onChange={(e) => setForm({ ...form, targetDate: e.target.value })}
          />
        </label>
        <button className="primary-btn">
          {editing ? "Update goal" : "Create goal"}
        </button>
      </form>
      <div className="dashboard-grid">
        {data?.map((g) => (
          <div className="panel" key={g.id}>
            <h2>{g.name}</h2>
            <p>
              {money(g.current_amount)} / {money(g.target_amount)}
            </p>
            <div className="progress-track">
              <div style={{ width: g.progressPercent + "%" }} />
            </div>
            <p>
              {g.progressPercent}% complete · {money(g.requiredMonthlySavings)}
              /month needed
            </p>
            <p>
              Estimated completion:{" "}
              {g.estimatedCompletion || "More positive savings history needed"}
            </p>
            <button
              onClick={() => {
                setEditing(g.id);
                setForm({
                  name: g.name,
                  targetAmount: g.target_amount,
                  currentAmount: g.current_amount,
                  targetDate: g.target_date,
                  version: g.version,
                });
              }}
            >
              Update progress
            </button>{" "}
            <button
              onClick={async () => {
                try {
                  await request("/goals/" + g.id, { method: "DELETE" });
                  load();
                } catch (e) {
                  setFailure(e.message);
                }
              }}
            >
              Delete
            </button>
          </div>
        ))}
      </div>
    </>
  );
}
