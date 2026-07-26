import { useEffect, useMemo, useState } from 'react'
import { api } from './api.js'

const NAV_ITEMS = [
  ['dashboard', '⌂', 'Dashboard'],
  ['pay', '₹', 'Pay'],
  ['transactions', '↔', 'Transactions'],
  ['budgets', '◔', 'Budgets'],
  ['categories', '◇', 'Categories'],
  ['reports', '▥', 'Reports'],
  ['assistant', '✦', 'AI Assistant'],
]

const money = (value = 0) => `₹${Number(value).toLocaleString('en-IN', {
  maximumFractionDigits: 0,
})}`

const date = (value) => new Date(value).toLocaleString('en-IN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

function App() {
  const [page, setPage] = useState('dashboard')
  const [user, setUser] = useState(null)
  const [authLoading, setAuthLoading] = useState(true)

  useEffect(() => {
    api.me()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setAuthLoading(false))
  }, [])

  if (authLoading) {
    return <div className="auth-loading">Opening SpendWise…</div>
  }

  if (!user) {
    return <AuthPage onAuthenticated={setUser} />
  }

  const logout = async () => {
    try {
      await api.logout()
    } finally {
      setUser(null)
      setPage('dashboard')
    }
  }

  return (
    <div className="app-shell">
      <Sidebar page={page} setPage={setPage} user={user} onLogout={logout} />
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

function AuthPage({ onAuthenticated }) {
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const response = mode === 'login'
        ? await api.login({ email: form.email, password: form.password })
        : await api.register(form)
      onAuthenticated(response)
    } catch (exception) {
      setError(exception.message)
    } finally {
      setLoading(false)
    }
  }

  const switchMode = () => {
    setMode((current) => current === 'login' ? 'register' : 'login')
    setError('')
  }

  return (
    <div className="auth-page">
      <section className="auth-hero">
        <div className="brand auth-brand">
          <div className="brand-mark">S</div>
          <div><strong>SpendWise</strong><span>AI money manager</span></div>
        </div>
        <div>
          <span className="eyebrow">Secure personal finance</span>
          <h1>Pay in test mode. Understand every rupee.</h1>
          <p>Each account gets private transactions, categories, budgets, reports, and AI learning.</p>
        </div>
        <div className="auth-feature-list">
          <span>✓ Passwords protected with BCrypt</span>
          <span>✓ Signed login cookie</span>
          <span>✓ User-specific financial data</span>
        </div>
      </section>

      <section className="auth-form-wrap">
        <form className="auth-card" onSubmit={submit}>
          <span className="eyebrow">{mode === 'login' ? 'Welcome back' : 'Create your account'}</span>
          <h2>{mode === 'login' ? 'Log in to SpendWise' : 'Start managing smarter'}</h2>
          <p>{mode === 'login' ? 'Use the demo account or your registered account.' : 'A demo wallet and prepared categories will be created automatically.'}</p>

          {mode === 'register' && (
            <label>
              Name
              <input
                required
                minLength="2"
                value={form.name}
                onChange={(event) => setForm({ ...form, name: event.target.value })}
                placeholder="Anmay Rai"
              />
            </label>
          )}

          <label>
            Email
            <input
              required
              type="email"
              value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })}
              placeholder="you@example.com"
            />
          </label>

          <label>
            Password
            <input
              required
              minLength="8"
              type="password"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              placeholder="At least 8 characters"
            />
          </label>

          {error && <div className="inline-error">{error}</div>}
          <button className="primary-btn large" disabled={loading}>
            {loading ? 'Please wait…' : mode === 'login' ? 'Log in' : 'Create account'}
          </button>

          {api.googleLoginEnabled && (
            <a className="google-login-btn" href={api.googleLoginUrl}>Continue with Google</a>
          )}

          <button className="auth-switch" type="button" onClick={switchMode}>
            {mode === 'login' ? 'New here? Create an account' : 'Already registered? Log in'}
          </button>

          {mode === 'login' && (
            <div className="demo-credentials">
              <strong>Demo login</strong>
              <span>demo@spendwise.local</span>
              <span>Demo@123</span>
            </div>
          )}
        </form>
      </section>
    </div>
  )
}

function Sidebar({ page, setPage, user, onLogout }) {
  const initial = user.name?.trim()?.charAt(0)?.toUpperCase() || 'U'
  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark">S</div>
        <div><strong>SpendWise</strong><span>AI money manager</span></div>
      </div>
      <nav>
        {NAV_ITEMS.map(([id, icon, label]) => (
          <button
            key={id}
            className={page === id ? 'nav-item active' : 'nav-item'}
            onClick={() => setPage(id)}
          >
            <span>{icon}</span>{label}
          </button>
        ))}
      </nav>
      <div className="demo-user authenticated-user">
        <div className="avatar">{initial}</div>
        <div className="user-copy"><strong>{user.name}</strong><span>{user.email}</span></div>
        <button className="logout-btn" onClick={onLogout} title="Log out">↪</button>
      </div>
    </aside>
  )
}

function MobileHeader({ page }) {
  const label = NAV_ITEMS.find(([id]) => id === page)?.[2] || 'SpendWise'
  return <div className="mobile-header"><strong>SpendWise AI</strong><span>{label}</span></div>
}

function PageHeader({ eyebrow, title, description, action }) {
  return (
    <header className="page-header">
      <div>
        <span className="eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {action}
    </header>
  )
}

function Loading() {
  return <div className="state-card">Loading your money data…</div>
}

function ErrorCard({ error }) {
  return <div className="state-card error">{error}</div>
}

function DashboardPage({ onNavigate }) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.dashboard().then(setData).catch((exception) => setError(exception.message))
  }, [])

  if (error) return <ErrorCard error={error} />
  if (!data) return <Loading />

  return (
    <>
      <PageHeader
        eyebrow="Overview"
        title={`Good to see you, ${data.userName}`}
        description="Your wallet, budgets, and AI-classified spending in one place."
        action={<button className="primary-btn" onClick={() => onNavigate('pay')}>＋ Make payment</button>}
      />
      <section className="stats-grid">
        <StatCard label="Demo wallet" value={money(data.walletBalance)} hint="Available balance" />
        <StatCard label="Spent this month" value={money(data.totalSpentThisMonth)} hint={`${data.categorySpending.length} active categories`} />
        <StatCard label="Remaining budget" value={money(data.remainingBudget)} hint={`From ${money(data.totalBudget)} configured`} />
      </section>
      <section className="dashboard-grid">
        <div className="panel spending-panel">
          <PanelTitle title="Category spending" subtitle="Current month" />
          <SpendingVisual categories={data.categorySpending} />
        </div>
        <div className="panel">
          <PanelTitle title="Budget watch" subtitle="Projected using this month's pace" />
          <div className="budget-list">
            {data.budgets.map((budget) => <BudgetBar key={budget.budgetId} budget={budget} />)}
          </div>
        </div>
      </section>
      <section className="panel">
        <PanelTitle
          title="Recent transactions"
          subtitle="AI suggestion and final category"
          action={<button className="text-btn" onClick={() => onNavigate('transactions')}>View all →</button>}
        />
        <TransactionTable transactions={data.recentTransactions} />
      </section>
    </>
  )
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

  return (
    <div className="spending-visual">
      <div className="donut" style={{ background: segments }}>
        <div><strong>{categories.length}</strong><span>categories</span></div>
      </div>
      <div className="legend">
        {categories.slice(0, 6).map((item) => (
          <div key={item.category}>
            <span>{item.icon} {item.category}</span>
            <strong>{money(item.amount)}</strong>
          </div>
        ))}
      </div>
    </div>
  )
}

function BudgetBar({ budget }) {
  const width = Math.min(budget.usedPercent, 100)
  return (
    <div className={budget.warning ? 'budget-row warning-row' : 'budget-row'}>
      <div className="budget-heading">
        <span>{budget.icon} {budget.category}</span>
        <strong>{money(budget.spent)} / {money(budget.limit)}</strong>
      </div>
      <div className="progress-track"><div style={{ width: `${width}%` }} /></div>
      <div className="budget-meta">
        <span>{budget.usedPercent}% used</span>
        <span>Projected {money(budget.projectedSpend)}</span>
      </div>
    </div>
  )
}

function TransactionTable({ transactions, categories, onCategoryChange }) {
  if (!transactions.length) return <div className="empty">No transactions yet.</div>

  return (
    <div className="transaction-list">
      {transactions.map((transaction) => (
        <div className="transaction-row" key={transaction.id}>
          <div className="transaction-icon">{transaction.categoryIcon}</div>
          <div className="transaction-main">
            <strong>{transaction.merchant}</strong>
            <span>
              {date(transaction.occurredAt)} · AI: {transaction.aiSuggestedCategory}
              {' '}({Math.round(transaction.aiConfidence * 100)}%)
            </span>
          </div>
          {categories ? (
            <select
              value={transaction.categoryId}
              onChange={(event) => onCategoryChange(transaction.id, Number(event.target.value))}
            >
              {categories.map((category) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
          ) : (
            <span className="category-pill">{transaction.category}</span>
          )}
          <strong className="amount">−{money(transaction.amount)}</strong>
        </div>
      ))}
    </div>
  )
}

function PayPage({ onDone }) {
  const [form, setForm] = useState({ merchant: '', amount: '', description: '' })
  const [categories, setCategories] = useState([])
  const [preview, setPreview] = useState(null)
  const [selectedCategoryId, setSelectedCategoryId] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [action, setAction] = useState('')
  const [customCategory, setCustomCategory] = useState({ name: '', icon: '🏷️' })

  useEffect(() => {
    api.categories()
      .then(setCategories)
      .catch((exception) => setError(exception.message))
  }, [])

  const updateForm = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }))
    setPreview(null)
    setSelectedCategoryId('')
    setResult(null)
    setError('')
  }

  const getSuggestion = async (event) => {
    event.preventDefault()
    setError('')
    setResult(null)
    setAction('preview')

    try {
      const response = await api.previewPayment({
        merchant: form.merchant,
        amount: Number(form.amount),
        description: form.description,
      })
      setPreview(response)
      setSelectedCategoryId(String(response.suggestedCategoryId))
    } catch (exception) {
      setError(exception.message)
    } finally {
      setAction('')
    }
  }

  const createCustomCategory = async () => {
    if (!customCategory.name.trim()) {
      setError('Enter a custom category name.')
      return
    }

    setError('')
    setAction('category')
    try {
      const created = await api.createCategory({
        name: customCategory.name,
        icon: customCategory.icon,
      })
      setCategories((items) => [...items, created].sort((a, b) => a.name.localeCompare(b.name)))
      setSelectedCategoryId(String(created.id))
      setCustomCategory({ name: '', icon: '🏷️' })
    } catch (exception) {
      setError(exception.message)
    } finally {
      setAction('')
    }
  }

  const confirmPayment = async () => {
    if (!selectedCategoryId) {
      setError('Choose a category before confirming the test payment.')
      return
    }

    setError('')
    setAction('confirm')
    try {
      const response = await api.confirmPayment({
        merchant: form.merchant,
        amount: Number(form.amount),
        description: form.description,
        categoryId: Number(selectedCategoryId),
      })
      setResult(response)
    } catch (exception) {
      setError(exception.message)
    } finally {
      setAction('')
    }
  }

  const editDetails = () => {
    setPreview(null)
    setSelectedCategoryId('')
    setError('')
  }

  return (
    <>
      <PageHeader
        eyebrow="Test payment flow"
        title="Review the category before paying"
        description="AI suggests a category first. You can change it or create a new category before the simulated payment is completed."
      />

      <div className="test-mode-banner">
        <strong>TEST MODE</strong>
        <span>No real money, card, bank account, or UPI transfer is involved yet.</span>
      </div>

      <div className="payment-flow-steps">
        <span className={!preview && !result ? 'active' : 'complete'}>1. Payment details</span>
        <span className={preview && !result ? 'active' : result ? 'complete' : ''}>2. Confirm category</span>
        <span className={result ? 'active complete' : ''}>3. Test payment</span>
      </div>

      <div className="two-column-form">
        <form className="panel form-card" onSubmit={getSuggestion}>
          <h2>Payment details</h2>
          <label>
            Merchant name
            <input
              required
              disabled={Boolean(preview || result)}
              value={form.merchant}
              onChange={(event) => updateForm('merchant', event.target.value)}
              placeholder="Pizza Hut"
            />
          </label>
          <label>
            Amount
            <input
              required
              disabled={Boolean(preview || result)}
              min="1"
              step="0.01"
              type="number"
              value={form.amount}
              onChange={(event) => updateForm('amount', event.target.value)}
              placeholder="650"
            />
          </label>
          <label>
            Description
            <textarea
              disabled={Boolean(preview || result)}
              value={form.description}
              onChange={(event) => updateForm('description', event.target.value)}
              placeholder="Dinner order"
            />
          </label>

          {error && <div className="inline-error">{error}</div>}

          {!preview && !result && (
            <button className="primary-btn large" disabled={action === 'preview'}>
              {action === 'preview' ? 'Getting AI suggestion…' : 'Continue to category'}
            </button>
          )}

          {preview && !result && (
            <button className="secondary-btn" type="button" onClick={editDetails}>
              ← Edit payment details
            </button>
          )}
        </form>

        <div className="panel result-card payment-review-card">
          {!preview && !result && (
            <div className="result-placeholder">
              <div>✦</div>
              <h2>Category comes before payment</h2>
              <p>Enter the merchant and amount. The next screen will let you accept or change the AI suggestion.</p>
            </div>
          )}

          {preview && !result && (
            <div className="category-review">
              <span className="review-label">AI suggestion</span>
              <div className="suggestion-card">
                <div className="suggestion-icon">{preview.suggestedCategoryIcon}</div>
                <div>
                  <strong>{preview.suggestedCategory}</strong>
                  <span>{Math.round(preview.confidence * 100)}% confidence · {preview.predictionSource}</span>
                </div>
              </div>

              <label>
                Final category
                <select
                  value={selectedCategoryId}
                  onChange={(event) => setSelectedCategoryId(event.target.value)}
                >
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.icon} {category.name}
                    </option>
                  ))}
                </select>
              </label>

              <div className="custom-category-box">
                <strong>Create a category during payment</strong>
                <div className="custom-category-fields">
                  <input
                    aria-label="Category icon"
                    className="icon-input"
                    value={customCategory.icon}
                    onChange={(event) => setCustomCategory((current) => ({
                      ...current,
                      icon: event.target.value,
                    }))}
                  />
                  <input
                    aria-label="Category name"
                    value={customCategory.name}
                    onChange={(event) => setCustomCategory((current) => ({
                      ...current,
                      name: event.target.value,
                    }))}
                    placeholder="Example: Weekend outings"
                  />
                  <button
                    className="secondary-btn"
                    type="button"
                    disabled={action === 'category'}
                    onClick={createCustomCategory}
                  >
                    {action === 'category' ? 'Creating…' : 'Create'}
                  </button>
                </div>
              </div>

              <div className="payment-summary">
                <span>Test payment to</span>
                <strong>{preview.merchant}</strong>
                <b>{money(preview.amount)}</b>
              </div>

              <button
                className="primary-btn large"
                type="button"
                disabled={action === 'confirm'}
                onClick={confirmPayment}
              >
                {action === 'confirm' ? 'Completing test payment…' : `Confirm test payment ${money(preview.amount)}`}
              </button>
            </div>
          )}

          {result && (
            <div className="success-result">
              <div className="success-mark">✓</div>
              <span>Test payment successful</span>
              <h2>{money(result.amount)} to {result.merchant}</h2>
              <div className="prediction">
                <span>Final category</span>
                <strong>{result.categoryIcon} {result.category}</strong>
                <small>AI originally suggested {result.aiSuggestedCategory}</small>
              </div>
              {result.budgetWarning && <div className="warning-box">⚠ {result.budgetWarning}</div>}
              <p>Demo wallet balance: <strong>{money(result.walletBalance)}</strong></p>
              <button className="secondary-btn" onClick={onDone}>Back to dashboard</button>
            </div>
          )}
        </div>
      </div>
    </>
  )
}

function TransactionsPage() {
  const [transactions, setTransactions] = useState([])
  const [categories, setCategories] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([api.transactions(), api.categories()])
      .then(([transactionData, categoryData]) => {
        setTransactions(transactionData)
        setCategories(categoryData)
      })
      .catch((exception) => setError(exception.message))
  }, [])

  const change = async (transactionId, categoryId) => {
    try {
      const updated = await api.updateTransactionCategory(transactionId, categoryId)
      setTransactions((items) => items.map((transaction) => (
        transaction.id === transactionId ? updated : transaction
      )))
    } catch (exception) {
      setError(exception.message)
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Activity"
        title="Transactions"
        description="Correct a category here. The ML service remembers that merchant choice for the demo user."
      />
      {error && <ErrorCard error={error} />}
      <section className="panel">
        <TransactionTable
          transactions={transactions}
          categories={categories}
          onCategoryChange={change}
        />
      </section>
    </>
  )
}

function BudgetsPage() {
  const [budgets, setBudgets] = useState([])
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState({ categoryId: '', monthlyLimit: '', warningPercent: 80 })
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([api.budgets(), api.categories()])
      .then(([budgetData, categoryData]) => {
        setBudgets(budgetData)
        setCategories(categoryData)
        if (categoryData[0]) {
          setForm((current) => (
            current.categoryId ? current : { ...current, categoryId: categoryData[0].id }
          ))
        }
      })
      .catch((exception) => setError(exception.message))
  }, [])

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    try {
      setBudgets(await api.setBudget({
        categoryId: Number(form.categoryId),
        monthlyLimit: Number(form.monthlyLimit),
        warningPercent: Number(form.warningPercent),
      }))
      setMessage('Budget saved.')
      setForm((current) => ({ ...current, monthlyLimit: '' }))
    } catch (exception) {
      setError(exception.message)
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Controls"
        title="Monthly budgets"
        description="Limits are optional. Warnings appear when a category reaches your chosen percentage."
      />
      <div className="content-split">
        <section className="panel">
          <PanelTitle title="Current limits" subtitle="Live spend and projected month-end value" />
          <div className="budget-list">
            {budgets.map((budget) => <BudgetBar key={budget.budgetId} budget={budget} />)}
          </div>
        </section>
        <form className="panel form-card compact" onSubmit={submit}>
          <h2>Set or update budget</h2>
          <label>
            Category
            <select value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>{category.icon} {category.name}</option>
              ))}
            </select>
          </label>
          <label>
            Monthly limit
            <input required type="number" min="1" value={form.monthlyLimit} onChange={(event) => setForm({ ...form, monthlyLimit: event.target.value })} placeholder="5000" />
          </label>
          <label>
            Warn at
            <input required type="number" min="1" max="100" value={form.warningPercent} onChange={(event) => setForm({ ...form, warningPercent: event.target.value })} />
          </label>
          {message && <div className="success-note">{message}</div>}
          {error && <div className="inline-error">{error}</div>}
          <button className="primary-btn">Save budget</button>
        </form>
      </div>
    </>
  )
}

function CategoriesPage() {
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState({ name: '', icon: '🏷️' })
  const [error, setError] = useState('')

  useEffect(() => {
    api.categories().then(setCategories).catch((exception) => setError(exception.message))
  }, [])

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    try {
      const created = await api.createCategory({ ...form })
      setCategories((items) => [...items, created].sort((a, b) => a.name.localeCompare(b.name)))
      setForm({ name: '', icon: '🏷️' })
    } catch (exception) {
      setError(exception.message)
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Personalize"
        title="Expense categories"
        description="Use prepared categories or create categories specific to your life."
      />
      <div className="content-split">
        <section className="category-grid">
          {categories.map((category) => (
            <div className="category-card" key={category.id}>
              <span>{category.icon}</span>
              <strong>{category.name}</strong>
              <small>{category.systemDefined ? 'Prepared' : 'Custom'}</small>
            </div>
          ))}
        </section>
        <form className="panel form-card compact" onSubmit={submit}>
          <h2>Create category</h2>
          <label>
            Name
            <input required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="College expenses" />
          </label>
          <label>
            Icon
            <input required value={form.icon} onChange={(event) => setForm({ ...form, icon: event.target.value })} />
          </label>
          {error && <div className="inline-error">{error}</div>}
          <button className="primary-btn">Create category</button>
        </form>
      </div>
    </>
  )
}

function ReportsPage() {
  const [report, setReport] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.report().then(setReport).catch((exception) => setError(exception.message))
  }, [])

  if (error) return <ErrorCard error={error} />
  if (!report) return <Loading />

  return (
    <>
      <PageHeader
        eyebrow="Monthly intelligence"
        title="Spending report"
        description="A database-calculated report with readable insights."
      />
      <section className="stats-grid">
        <StatCard label="Total spent" value={money(report.totalSpent)} hint={`${report.changePercent >= 0 ? '+' : ''}${report.changePercent}% vs previous month`} />
        <StatCard label="Previous month" value={money(report.previousMonthSpent)} hint="Comparison baseline" />
        <StatCard label="Highest category" value={report.highestCategory} hint={money(report.highestCategoryAmount)} />
      </section>
      <section className="dashboard-grid">
        <div className="panel">
          <PanelTitle title="Breakdown" subtitle={`${report.month}/${report.year}`} />
          <div className="report-bars">
            {report.categorySpending.map((item) => (
              <div key={item.category}>
                <div><span>{item.icon} {item.category}</span><strong>{money(item.amount)}</strong></div>
                <div className="report-track"><span style={{ width: `${item.percentage}%` }} /></div>
              </div>
            ))}
          </div>
        </div>
        <div className="panel insight-panel">
          <PanelTitle title="Smart insights" subtitle="Generated from your actual data" />
          {report.insights.map((insight) => (
            <div className="insight" key={insight}>✦ <span>{insight}</span></div>
          ))}
        </div>
      </section>
    </>
  )
}

function AssistantPage() {
  const starters = [
    'Where did I spend the most this month?',
    'How much did I spend on food last week?',
    'Compare this month with last month.',
    'Can I spend ₹3,000 this month?',
  ]
  const [messages, setMessages] = useState([{
    role: 'assistant',
    text: 'Ask me about your stored spending. I calculate answers from the database instead of guessing.',
    facts: [],
  }])
  const [question, setQuestion] = useState('')
  const [loading, setLoading] = useState(false)

  const ask = async (text) => {
    const value = text || question
    if (!value.trim()) return

    setQuestion('')
    setMessages((items) => [...items, { role: 'user', text: value }])
    setLoading(true)

    try {
      const response = await api.ask(value)
      setMessages((items) => [...items, {
        role: 'assistant',
        text: response.answer,
        facts: response.facts,
      }])
    } catch (exception) {
      setMessages((items) => [...items, {
        role: 'assistant',
        text: exception.message,
        facts: [],
      }])
    } finally {
      setLoading(false)
    }
  }

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
                {message.facts?.map((fact) => <small key={fact}>{fact}</small>)}
              </div>
            ))}
            {loading && <div className="message assistant"><p>Checking your transactions…</p></div>}
          </div>
          <div className="chat-input">
            <input
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              onKeyDown={(event) => event.key === 'Enter' && ask()}
              placeholder="Ask about your spending…"
            />
            <button className="primary-btn" onClick={() => ask()}>Ask</button>
          </div>
        </div>
        <div className="starter-list">
          <h3>Try these</h3>
          {starters.map((starter) => (
            <button key={starter} onClick={() => ask(starter)}>{starter}<span>→</span></button>
          ))}
        </div>
      </section>
    </>
  )
}

export default App
