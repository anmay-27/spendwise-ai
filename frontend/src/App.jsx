import { useEffect, useMemo, useState } from 'react'
import { api } from './api'

const NAV_ITEMS = [
  ['dashboard', '⌂', 'Dashboard'],
  ['pay', '₹', 'Pay'],
  ['transactions', '↔', 'Transactions'],
  ['budgets', '◔', 'Budgets'],
  ['categories', '◇', 'Categories'],
  ['reports', '▥', 'Reports'],
  ['assistant', '✦', 'AI Assistant'],
]

const money = (value = 0) => `₹${Number(value).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`
const date = (value) => new Date(value).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })

function App() {
  const [page, setPage] = useState('dashboard')
  return (
    <div className="app-shell">
      <Sidebar page={page} setPage={setPage} />
      <main className="main-content">
        <MobileHeader page={page} />
        {page === 'dashboard' && <DashboardPage onNavigate={setPage} />}
        {page === 'pay' && <PayPage onDone={() => setPage('dashboard')} />}
        {page === 'transactions' && <TransactionsPage />}
        {page === 'budgets' && <BudgetsPage />}
        {page === 'categories' && <CategoriesPage />}
        {page === 'reports' && <ReportsPage />}
        {page === 'assistant' && <AssistantPage />}
      </main>
    </div>
  )
}

function Sidebar({ page, setPage }) {
  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark">S</div>
        <div><strong>SpendWise</strong><span>AI money manager</span></div>
      </div>
      <nav>
        {NAV_ITEMS.map(([id, icon, label]) => (
          <button key={id} className={page === id ? 'nav-item active' : 'nav-item'} onClick={() => setPage(id)}>
            <span>{icon}</span>{label}
          </button>
        ))}
      </nav>
      <div className="demo-user"><div className="avatar">A</div><div><strong>Anmay</strong><span>Demo account</span></div></div>
    </aside>
  )
}

function MobileHeader({ page }) {
  const label = NAV_ITEMS.find(([id]) => id === page)?.[2] || 'SpendWise'
  return <div className="mobile-header"><strong>SpendWise AI</strong><span>{label}</span></div>
}

function PageHeader({ eyebrow, title, description, action }) {
  return <header className="page-header"><div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{description}</p></div>{action}</header>
}

function Loading() { return <div className="state-card">Loading your money data…</div> }
function ErrorCard({ error }) { return <div className="state-card error">{error}</div> }

function DashboardPage({ onNavigate }) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const load = () => api.dashboard().then(setData).catch((e) => setError(e.message))
  useEffect(load, [])
  if (error) return <ErrorCard error={error} />
  if (!data) return <Loading />

  return <>
    <PageHeader eyebrow="Overview" title={`Good to see you, ${data.userName}`} description="Your wallet, budgets, and AI-classified spending in one place." action={<button className="primary-btn" onClick={() => onNavigate('pay')}>＋ Make payment</button>} />
    <section className="stats-grid">
      <StatCard label="Demo wallet" value={money(data.walletBalance)} hint="Available balance" />
      <StatCard label="Spent this month" value={money(data.totalSpentThisMonth)} hint={`${data.categorySpending.length} active categories`} />
      <StatCard label="Remaining budget" value={money(data.remainingBudget)} hint={`From ${money(data.totalBudget)} configured`} />
    </section>
    <section className="dashboard-grid">
      <div className="panel spending-panel"><PanelTitle title="Category spending" subtitle="Current month" />
        <SpendingVisual categories={data.categorySpending} />
      </div>
      <div className="panel"><PanelTitle title="Budget watch" subtitle="Projected using this month's pace" />
        <div className="budget-list">{data.budgets.map((budget) => <BudgetBar key={budget.budgetId} budget={budget} />)}</div>
      </div>
    </section>
    <section className="panel"><PanelTitle title="Recent transactions" subtitle="AI suggestion and final category" action={<button className="text-btn" onClick={() => onNavigate('transactions')}>View all →</button>} />
      <TransactionTable transactions={data.recentTransactions} />
    </section>
  </>
}

function StatCard({ label, value, hint }) {
  return <div className="stat-card"><span>{label}</span><strong>{value}</strong><small>{hint}</small></div>
}

function PanelTitle({ title, subtitle, action }) {
  return <div className="panel-title"><div><h2>{title}</h2><p>{subtitle}</p></div>{action}</div>
}

function SpendingVisual({ categories }) {
  const segments = useMemo(() => {
    let cursor = 0
    const palette = ['#7c3aed', '#06b6d4', '#f59e0b', '#ec4899', '#22c55e', '#6366f1', '#ef4444']
    const parts = categories.map((item, index) => {
      const start = cursor
      cursor += item.percentage
      return `${palette[index % palette.length]} ${start}% ${cursor}%`
    })
    return parts.length ? `conic-gradient(${parts.join(',')})` : '#e5e7eb'
  }, [categories])
  return <div className="spending-visual"><div className="donut" style={{ background: segments }}><div><strong>{categories.length}</strong><span>categories</span></div></div>
    <div className="legend">{categories.slice(0, 6).map((item) => <div key={item.category}><span>{item.icon} {item.category}</span><strong>{money(item.amount)}</strong></div>)}</div></div>
}

function BudgetBar({ budget }) {
  const width = Math.min(budget.usedPercent, 100)
  return <div className={budget.warning ? 'budget-row warning-row' : 'budget-row'}>
    <div className="budget-heading"><span>{budget.icon} {budget.category}</span><strong>{money(budget.spent)} / {money(budget.limit)}</strong></div>
    <div className="progress-track"><div style={{ width: `${width}%` }} /></div>
    <div className="budget-meta"><span>{budget.usedPercent}% used</span><span>Projected {money(budget.projectedSpend)}</span></div>
  </div>
}

function TransactionTable({ transactions, categories, onCategoryChange }) {
  if (!transactions.length) return <div className="empty">No transactions yet.</div>
  return <div className="transaction-list">{transactions.map((tx) => <div className="transaction-row" key={tx.id}>
    <div className="transaction-icon">{tx.categoryIcon}</div>
    <div className="transaction-main"><strong>{tx.merchant}</strong><span>{date(tx.occurredAt)} · AI: {tx.aiSuggestedCategory} ({Math.round(tx.aiConfidence * 100)}%)</span></div>
    {categories ? <select value={tx.categoryId} onChange={(e) => onCategoryChange(tx.id, Number(e.target.value))}>{categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select> : <span className="category-pill">{tx.category}</span>}
    <strong className="amount">−{money(tx.amount)}</strong>
  </div>)}</div>
}

function PayPage({ onDone }) {
  const [form, setForm] = useState({ merchant: '', amount: '', description: '' })
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const submit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true); setResult(null)
    try { setResult(await api.pay({ userId: 1, merchant: form.merchant, amount: Number(form.amount), description: form.description })) }
    catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  return <>
    <PageHeader eyebrow="Demo payment" title="Pay and categorize instantly" description="This simulates a wallet payment. No real money is transferred." />
    <div className="two-column-form">
      <form className="panel form-card" onSubmit={submit}>
        <label>Merchant name<input required value={form.merchant} onChange={(e) => setForm({ ...form, merchant: e.target.value })} placeholder="PVR Cinemas" /></label>
        <label>Amount<input required min="1" step="0.01" type="number" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} placeholder="650" /></label>
        <label>Description<textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="Movie tickets" /></label>
        {error && <div className="inline-error">{error}</div>}
        <button className="primary-btn large" disabled={loading}>{loading ? 'Predicting category…' : `Pay ${form.amount ? money(form.amount) : ''}`}</button>
      </form>
      <div className="panel result-card">
        {!result ? <div className="result-placeholder"><div>✦</div><h2>AI category preview</h2><p>The ML service uses merchant and description text, then the backend checks your category budget.</p></div> : <div className="success-result">
          <div className="success-mark">✓</div><span>Payment successful</span><h2>{money(result.amount)} to {result.merchant}</h2>
          <div className="prediction"><span>AI category</span><strong>{result.categoryIcon} {result.category}</strong><small>{Math.round(result.confidence * 100)}% confidence</small></div>
          {result.budgetWarning && <div className="warning-box">⚠ {result.budgetWarning}</div>}
          <p>Wallet balance: <strong>{money(result.walletBalance)}</strong></p>
          <button className="secondary-btn" onClick={onDone}>Back to dashboard</button>
        </div>}
      </div>
    </div>
  </>
}

function TransactionsPage() {
  const [transactions, setTransactions] = useState([])
  const [categories, setCategories] = useState([])
  const [error, setError] = useState('')
  const load = () => Promise.all([api.transactions(), api.categories()]).then(([t, c]) => { setTransactions(t); setCategories(c) }).catch((e) => setError(e.message))
  useEffect(load, [])
  const change = async (transactionId, categoryId) => {
    try { const updated = await api.updateTransactionCategory(transactionId, categoryId); setTransactions((items) => items.map((tx) => tx.id === transactionId ? updated : tx)) }
    catch (e) { setError(e.message) }
  }
  return <><PageHeader eyebrow="Activity" title="Transactions" description="Correct a category here. The ML service remembers that merchant choice for the demo user." />
    {error && <ErrorCard error={error} />}<section className="panel"><TransactionTable transactions={transactions} categories={categories} onCategoryChange={change} /></section></>
}

function BudgetsPage() {
  const [budgets, setBudgets] = useState([])
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState({ categoryId: '', monthlyLimit: '', warningPercent: 80 })
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const load = () => Promise.all([api.budgets(), api.categories()]).then(([b, c]) => { setBudgets(b); setCategories(c); if (!form.categoryId && c[0]) setForm((f) => ({ ...f, categoryId: c[0].id })) }).catch((e) => setError(e.message))
  useEffect(load, [])
  const submit = async (e) => { e.preventDefault(); setError(''); try { setBudgets(await api.setBudget({ userId: 1, categoryId: Number(form.categoryId), monthlyLimit: Number(form.monthlyLimit), warningPercent: Number(form.warningPercent) })); setMessage('Budget saved.'); setForm((f) => ({ ...f, monthlyLimit: '' })) } catch (e) { setError(e.message) } }
  return <><PageHeader eyebrow="Controls" title="Monthly budgets" description="Limits are optional. Warnings appear when a category reaches your chosen percentage." />
    <div className="content-split"><section className="panel"><PanelTitle title="Current limits" subtitle="Live spend and projected month-end value" /><div className="budget-list">{budgets.map((b) => <BudgetBar key={b.budgetId} budget={b} />)}</div></section>
      <form className="panel form-card compact" onSubmit={submit}><h2>Set or update budget</h2>
        <label>Category<select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>{categories.map((c) => <option key={c.id} value={c.id}>{c.icon} {c.name}</option>)}</select></label>
        <label>Monthly limit<input required type="number" min="1" value={form.monthlyLimit} onChange={(e) => setForm({ ...form, monthlyLimit: e.target.value })} placeholder="5000" /></label>
        <label>Warn at<input required type="number" min="1" max="100" value={form.warningPercent} onChange={(e) => setForm({ ...form, warningPercent: e.target.value })} /></label>
        {message && <div className="success-note">{message}</div>}{error && <div className="inline-error">{error}</div>}
        <button className="primary-btn">Save budget</button></form></div></>
}

function CategoriesPage() {
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState({ name: '', icon: '🏷️' })
  const [error, setError] = useState('')
  const load = () => api.categories().then(setCategories).catch((e) => setError(e.message))
  useEffect(load, [])
  const submit = async (e) => { e.preventDefault(); setError(''); try { const created = await api.createCategory({ userId: 1, ...form }); setCategories((items) => [...items, created].sort((a, b) => a.name.localeCompare(b.name))); setForm({ name: '', icon: '🏷️' }) } catch (e) { setError(e.message) } }
  return <><PageHeader eyebrow="Personalize" title="Expense categories" description="Use prepared categories or create categories specific to your life." />
    <div className="content-split"><section className="category-grid">{categories.map((category) => <div className="category-card" key={category.id}><span>{category.icon}</span><strong>{category.name}</strong><small>{category.systemDefined ? 'Prepared' : 'Custom'}</small></div>)}</section>
      <form className="panel form-card compact" onSubmit={submit}><h2>Create category</h2><label>Name<input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="College expenses" /></label><label>Icon<input required value={form.icon} onChange={(e) => setForm({ ...form, icon: e.target.value })} /></label>{error && <div className="inline-error">{error}</div>}<button className="primary-btn">Create category</button></form></div></>
}

function ReportsPage() {
  const [report, setReport] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => { api.report().then(setReport).catch((e) => setError(e.message)) }, [])
  if (error) return <ErrorCard error={error} />
  if (!report) return <Loading />
  return <><PageHeader eyebrow="Monthly intelligence" title="Spending report" description="A database-calculated report with readable insights." />
    <section className="stats-grid"><StatCard label="Total spent" value={money(report.totalSpent)} hint={`${report.changePercent >= 0 ? '+' : ''}${report.changePercent}% vs previous month`} /><StatCard label="Previous month" value={money(report.previousMonthSpent)} hint="Comparison baseline" /><StatCard label="Highest category" value={report.highestCategory} hint={money(report.highestCategoryAmount)} /></section>
    <section className="dashboard-grid"><div className="panel"><PanelTitle title="Breakdown" subtitle={`${report.month}/${report.year}`} /><div className="report-bars">{report.categorySpending.map((item) => <div key={item.category}><div><span>{item.icon} {item.category}</span><strong>{money(item.amount)}</strong></div><div className="report-track"><span style={{ width: `${item.percentage}%` }} /></div></div>)}</div></div>
      <div className="panel insight-panel"><PanelTitle title="Smart insights" subtitle="Generated from your actual data" />{report.insights.map((insight) => <div className="insight" key={insight}>✦ <span>{insight}</span></div>)}</div></section></>
}

function AssistantPage() {
  const starters = ['Where did I spend the most this month?', 'How much did I spend on food last week?', 'Compare this month with last month.', 'Can I spend ₹3,000 this month?']
  const [messages, setMessages] = useState([{ role: 'assistant', text: 'Ask me about your stored spending. I calculate answers from the database instead of guessing.', facts: [] }])
  const [question, setQuestion] = useState('')
  const [loading, setLoading] = useState(false)
  const ask = async (text) => { const value = text || question; if (!value.trim()) return; setQuestion(''); setMessages((m) => [...m, { role: 'user', text: value }]); setLoading(true); try { const response = await api.ask(value); setMessages((m) => [...m, { role: 'assistant', text: response.answer, facts: response.facts }]) } catch (e) { setMessages((m) => [...m, { role: 'assistant', text: e.message, facts: [] }]) } finally { setLoading(false) } }
  return <><PageHeader eyebrow="Conversational analytics" title="Ask SpendWise" description="Assistant v1 understands common budget and category questions without an external LLM key." />
    <section className="assistant-layout"><div className="panel chat-panel"><div className="messages">{messages.map((message, index) => <div key={index} className={`message ${message.role}`}><p>{message.text}</p>{message.facts?.map((fact) => <small key={fact}>{fact}</small>)}</div>)}{loading && <div className="message assistant"><p>Checking your transactions…</p></div>}</div><div className="chat-input"><input value={question} onChange={(e) => setQuestion(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && ask()} placeholder="Ask about your spending…" /><button className="primary-btn" onClick={() => ask()}>Ask</button></div></div>
      <div className="starter-list"><h3>Try these</h3>{starters.map((starter) => <button key={starter} onClick={() => ask(starter)}>{starter}<span>→</span></button>)}</div></section></>
}

export default App
