import { money } from "../lib/format";
export function Bars({ values }) {
  const entries = Object.entries(values || {}),
    max = Math.max(1, ...entries.map(([, v]) => Number(v)));
  return (
    <div className="report-bars">
      {entries.length ? (
        entries.map(([key, value]) => (
          <div key={key}>
            <div>
              <span>{key}</span>
              <strong>{money(value)}</strong>
            </div>
            <div className="report-track">
              <span style={{ width: (Number(value) / max) * 100 + "%" }} />
            </div>
          </div>
        ))
      ) : (
        <p>No data for this period.</p>
      )}
    </div>
  );
}
