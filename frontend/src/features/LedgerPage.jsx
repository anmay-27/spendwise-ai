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
export function LedgerPage() {
  const empty = {
    merchant: "",
    amount: "",
    type: "EXPENSE",
    categoryId: "",
    notes: "",
    occurredAt: new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
      .toISOString()
      .slice(0, 16),
    recurring: false,
  };
  const [form, setForm] = useState(empty),
    [editing, setEditing] = useState(null),
    [query, setQuery] = useState(""),
    [type, setType] = useState(""),
    [from, setFrom] = useState(""),
    [to, setTo] = useState(""),
    [category, setCategory] = useState(""),
    [sort, setSort] = useState("occurredAt"),
    [page, setPage] = useState(0),
    [error, setError] = useState("");
  const cats = useData("/categories");
  const result = useData(
    "/transactions/search?" +
      new URLSearchParams({
        query,
        page,
        size: 20,
        sort,
        direction: "desc",
        ...(type ? { type } : {}),
        ...(from ? { from } : {}),
        ...(to ? { to } : {}),
        ...(category ? { categoryId: category } : {}),
      }),
  );
  const save = async (e) => {
    e.preventDefault();
    try {
      await request("/transactions" + (editing ? "/" + editing : ""), {
        method: editing ? "PUT" : "POST",
        body: JSON.stringify({
          ...form,
          amount: Number(form.amount),
          categoryId: Number(form.categoryId),
          occurredAt:
            form.occurredAt.length === 16
              ? form.occurredAt + ":00"
              : form.occurredAt,
        }),
      });
      setForm(empty);
      setEditing(null);
      setError("");
      result.load();
    } catch (e) {
      setError(e.message);
    }
  };
  const remove = async (id) => {
    try {
      await request("/transactions/" + id, { method: "DELETE" });
      result.load();
    } catch (e) {
      setError(e.message);
    }
  };
  return (
    <>
      <PageHeader
        eyebrow="Activity"
        title="Transactions"
        description="Track income and expenses, search your history, and correct categories."
      />
      {(error || result.error) && <ErrorCard error={error || result.error} />}
      <form className="panel ledger-form" onSubmit={save}>
        <label>
          Merchant
          <input
            required
            maxLength={160}
            value={form.merchant}
            onChange={(e) => setForm({ ...form, merchant: e.target.value })}
          />
        </label>
        <label>
          Amount
          <input
            required
            type="number"
            min=".01"
            step=".01"
            value={form.amount}
            onChange={(e) => setForm({ ...form, amount: e.target.value })}
          />
        </label>
        <label>
          Type
          <select
            value={form.type}
            onChange={(e) => setForm({ ...form, type: e.target.value })}
          >
            <option>EXPENSE</option>
            <option>INCOME</option>
          </select>
        </label>
        <label>
          Category
          <select
            required
            value={form.categoryId}
            onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
          >
            <option value="">Select category</option>
            {cats.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Date
          <input
            required
            type="datetime-local"
            value={form.occurredAt}
            onChange={(e) => setForm({ ...form, occurredAt: e.target.value })}
          />
        </label>
        <label>
          Notes
          <input
            maxLength={500}
            value={form.notes || ""}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
          />
        </label>
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={form.recurring}
            onChange={(e) => setForm({ ...form, recurring: e.target.checked })}
          />{" "}
          Recurring
        </label>
        <button className="primary-btn">
          {editing ? "Save changes" : "Add transaction"}
        </button>
        {editing && (
          <button
            type="button"
            onClick={() => {
              setEditing(null);
              setForm(empty);
            }}
          >
            Cancel
          </button>
        )}
      </form>
      <div className="panel filters">
        <input
          aria-label="Search merchant or notes"
          placeholder="Search merchant or notes"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setPage(0);
          }}
        />
        <select
          aria-label="Filter type"
          value={type}
          onChange={(e) => {
            setType(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All types</option>
          <option>EXPENSE</option>
          <option>INCOME</option>
        </select>
        <select
          aria-label="Filter category"
          value={category}
          onChange={(e) => {
            setCategory(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All categories</option>
          {cats.data?.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <input
          aria-label="From date"
          type="date"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            setPage(0);
          }}
        />
        <input
          aria-label="To date"
          type="date"
          value={to}
          onChange={(e) => {
            setTo(e.target.value);
            setPage(0);
          }}
        />
        <select
          aria-label="Sort transactions"
          value={sort}
          onChange={(e) => {
            setSort(e.target.value);
            setPage(0);
          }}
        >
          <option value="occurredAt">Newest</option>
          <option value="amount">Largest</option>
          <option value="merchantName">Merchant</option>
        </select>
      </div>
      <section className="panel">
        {result.data?.content.map((tx) => (
          <div className="ledger-row" key={tx.id}>
            <div>
              <strong>{tx.merchant}</strong>
              <small>
                {tx.occurredAt.slice(0, 10)} · {tx.category}
              </small>
            </div>
            <strong className={tx.type === "INCOME" ? "income" : ""}>
              {tx.type === "INCOME" ? "+" : "−"}
              {money(tx.amount)}
            </strong>
            <select
              aria-label={"Category for " + tx.merchant}
              value={tx.categoryId}
              onChange={async (e) => {
                try {
                  await api.updateTransactionCategory(
                    tx.id,
                    Number(e.target.value),
                  );
                  result.load();
                } catch (e) {
                  setError(e.message);
                }
              }}
            >
              {cats.data?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
            {!tx.walletPayment && !tx.providerPayment && (
              <>
                <button
                  onClick={() => {
                    setEditing(tx.id);
                    setForm({
                      ...tx,
                      notes: tx.notes || "",
                      occurredAt: tx.occurredAt.slice(0, 16),
                    });
                  }}
                >
                  Edit
                </button>
                <button onClick={() => remove(tx.id)}>Delete</button>
              </>
            )}
          </div>
        ))}
        {result.data?.empty && <p>No matching transactions.</p>}
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(page - 1)}>
            Previous
          </button>
          <span>
            Page {page + 1} · {result.data?.totalElements || 0} transactions
          </span>
          <button
            disabled={result.data?.last !== false}
            onClick={() => setPage(page + 1)}
          >
            Next
          </button>
        </div>
      </section>
    </>
  );
}
