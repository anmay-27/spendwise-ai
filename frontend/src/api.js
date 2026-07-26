const API_BASE = import.meta.env.VITE_API_URL || '/api'
const BACKEND_BASE = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8081'
const GOOGLE_LOGIN_ENABLED = String(import.meta.env.VITE_GOOGLE_LOGIN_ENABLED || 'false') === 'true'

let csrf = null

function readCookie(name) {
  const prefix = `${name}=`
  const cookie = document.cookie
    .split('; ')
    .find((item) => item.startsWith(prefix))

  return cookie ? decodeURIComponent(cookie.substring(prefix.length)) : null
}

async function csrfToken() {
  if (csrf) return csrf

  const response = await fetch(`${API_BASE}/auth/csrf`, {
    credentials: 'include',
    cache: 'no-store',
  })

  if (!response.ok) {
    throw new Error('Could not initialize secure request protection')
  }

  const details = await response.json()
  const rawToken = readCookie('XSRF-TOKEN')

  if (!rawToken) {
    throw new Error('The CSRF security cookie was not created. Refresh the page and try again.')
  }

  csrf = {
    token: rawToken,
    headerName: details.headerName || 'X-XSRF-TOKEN',
  }

  return csrf
}

async function request(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase()
  const headers = {
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(options.headers || {}),
  }

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const token = await csrfToken()
    headers[token.headerName] = token.token
  }

  const response = await fetch(`${API_BASE}${path}`, {
    credentials: 'include',
    ...options,
    method,
    headers,
  })

  if (!response.ok) {
    let message = response.status === 401
      ? 'Please log in to continue'
      : `Request failed (${response.status})`

    try {
      const body = await response.json()
      message = body.message || message
    } catch {
      // Keep the readable fallback when the server did not return JSON.
    }

    throw new Error(message)
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}

async function authenticate(path, payload) {
  const result = await request(path, {
    method: 'POST',
    body: JSON.stringify(payload),
  })

  // Obtain a fresh CSRF cookie before the next state-changing request.
  csrf = null
  return result
}

export const api = {
  googleLoginEnabled: GOOGLE_LOGIN_ENABLED,
  googleLoginUrl: `${BACKEND_BASE}/oauth2/authorization/google`,

  me: () => request('/auth/me'),
  register: (payload) => authenticate('/auth/register', payload),
  login: (payload) => authenticate('/auth/login', payload),
  logout: async () => {
    await request('/auth/logout', { method: 'POST' })
    csrf = null
  },

  dashboard: () => request('/dashboard'),
  categories: () => request('/categories'),
  createCategory: (payload) => request('/categories', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),

  previewPayment: (payload) => request('/payments/preview', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),
  confirmPayment: (payload) => request('/payments/confirm', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),

  transactions: () => request('/transactions'),
  updateTransactionCategory: (transactionId, categoryId) => request(
    `/transactions/${transactionId}/category`,
    {
      method: 'PATCH',
      body: JSON.stringify({ categoryId }),
    },
  ),
  budgets: () => request('/budgets'),
  setBudget: (payload) => request('/budgets', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),
  report: () => request('/reports/monthly'),
  ask: (question) => request('/assistant/ask', {
    method: 'POST',
    body: JSON.stringify({ question }),
  }),
}
