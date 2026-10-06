import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { GuestOnly, RequireAuth } from './auth/RequireAuth'
import { AppShell } from './components/AppShell'
import { HabitsPage } from './habits/HabitsPage'
import { LoginPage } from './pages/LoginPage'
import { PrizesPage } from './prizes/PrizesPage'
import { RegisterPage } from './pages/RegisterPage'
import { TodayPage } from './today/TodayPage'

// Stats is the only page that uses Recharts, so it loads in its own chunk instead of slowing down first load.
const AnalyticsPage = lazy(() => import('./analytics/AnalyticsPage').then((m) => ({ default: m.AnalyticsPage })))

const pageLoading = (
  <AppShell>
    <p className="text-slate-400">Loading…</p>
  </AppShell>
)

function App() {
  return (
    <Routes>
      <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
      <Route path="/register" element={<GuestOnly><RegisterPage /></GuestOnly>} />
      <Route path="/" element={<RequireAuth><TodayPage /></RequireAuth>} />
      <Route path="/habits" element={<RequireAuth><HabitsPage /></RequireAuth>} />
      <Route path="/prizes" element={<RequireAuth><PrizesPage /></RequireAuth>} />
      <Route path="/analytics" element={<RequireAuth><Suspense fallback={pageLoading}><AnalyticsPage /></Suspense></RequireAuth>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
