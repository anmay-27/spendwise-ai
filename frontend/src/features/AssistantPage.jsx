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
export function AssistantPage() {
  const starters = [
    "Where did I spend the most this month?",
    "How much did I spend on food last week?",
    "Compare this month with last month.",
    "Can I spend ₹3,000 this month?",
  ];
  const [messages, setMessages] = useState([
    {
      role: "assistant",
      text: "Ask me about your stored spending. I calculate answers from the database instead of guessing.",
      facts: [],
    },
  ]);
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);
  const ask = async (text) => {
    const value = text || question;
    if (!value.trim()) return;
    setQuestion("");
    setMessages((m) => [...m, { role: "user", text: value }]);
    setLoading(true);
    try {
      const response = await api.ask(value);
      setMessages((m) => [
        ...m,
        { role: "assistant", text: response.answer, facts: response.facts },
      ]);
    } catch (e) {
      setMessages((m) => [
        ...m,
        { role: "assistant", text: e.message, facts: [] },
      ]);
    } finally {
      setLoading(false);
    }
  };
  return (
    <>
      <PageHeader
        eyebrow="Conversational analytics"
        title="Ask SpendWise"
        description="Assistant v1 understands common budget and category questions without an external LLM key."
      />
      <section className="assistant-layout">
        <div className="panel chat-panel">
          <div className="messages">
            {messages.map((message, index) => (
              <div key={index} className={`message ${message.role}`}>
                <p>{message.text}</p>
                {message.facts?.map((fact) => (
                  <small key={fact}>{fact}</small>
                ))}
              </div>
            ))}
            {loading && (
              <div className="message assistant">
                <p>Checking your transactions…</p>
              </div>
            )}
          </div>
          <div className="chat-input">
            <input
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && ask()}
              placeholder="Ask about your spending…"
            />
            <button className="primary-btn" onClick={() => ask()}>
              Ask
            </button>
          </div>
        </div>
        <div className="starter-list">
          <h3>Try these</h3>
          {starters.map((starter) => (
            <button key={starter} onClick={() => ask(starter)}>
              {starter}
              <span>→</span>
            </button>
          ))}
        </div>
      </section>
    </>
  );
}
