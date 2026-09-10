import { useEffect, useMemo, useState } from "react";
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
import { api } from "./api";
import { Sidebar, MobileHeader } from "./components/Layout";
import { AuthPage } from "./features/AuthPage";
import { DashboardPage } from "./features/DashboardPage";
import { PayPage } from "./features/PayPage";
import { BudgetsPage } from "./features/BudgetsPage";
import { CategoriesPage } from "./features/CategoriesPage";
import { ReportsPage } from "./features/ReportsPage";
import { AssistantPage } from "./features/AssistantPage";
import {
  FinanceOverview,
  LedgerPage,
  GoalsPage,
  DataPage,
} from "./features/FinancePages";

function App() {
  const [page, setPage] = useState("dashboard");
  const [user, setUser] = useState(undefined);
  const [theme, setTheme] = useState(() => {
    const saved = localStorage.getItem("spendwise-theme");
    if (saved === "light" || saved === "dark") return saved;
    return window.matchMedia("(prefers-color-scheme: dark)").matches
      ? "dark"
      : "light";
  });

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute("content", theme === "dark" ? "#120e10" : "#f7f4f5");
    localStorage.setItem("spendwise-theme", theme);
  }, [theme]);

  useEffect(() => {
    api
      .me()
      .then(setUser)
      .catch(() => setUser(null));
  }, []);

  const toggleTheme = () =>
    setTheme((current) => (current === "dark" ? "light" : "dark"));

  const logout = async () => {
    await api.logout();
    setUser(null);
    setPage("dashboard");
  };

  if (user === undefined)
    return (
      <div className="boot-screen">
        <div className="brand-mark">S</div>
      </div>
    );
  if (!user)
    return (
      <AuthPage
        onAuthenticated={setUser}
        theme={theme}
        toggleTheme={toggleTheme}
      />
    );

  return (
    <div className="app-shell">
      <Sidebar
        page={page}
        setPage={setPage}
        user={user}
        logout={logout}
        theme={theme}
        toggleTheme={toggleTheme}
      />
      <main className="main-content">
        <MobileHeader page={page} theme={theme} toggleTheme={toggleTheme} />
        {page === "dashboard" && (
          <>
            <FinanceOverview />
            <DashboardPage onNavigate={setPage} />
          </>
        )}
        {page === "pay" && <PayPage onDone={() => setPage("dashboard")} />}
        {page === "transactions" && <LedgerPage />}
        {page === "budgets" && <BudgetsPage />}
        {page === "categories" && <CategoriesPage />}
        {page === "reports" && <ReportsPage />}
        {page === "assistant" && <AssistantPage />}
        {page === "analytics" && <FinanceOverview full />}
        {page === "goals" && <GoalsPage />}
        {["subscriptions", "insights", "notifications"].includes(page) && (
          <DataPage key={page} kind={page} />
        )}
      </main>
    </div>
  );
}

export default App;
