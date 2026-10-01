import { Navigate, Route, Routes } from 'react-router'
import { GuestOnly, RequireAuth } from './auth/RequireAuth'
import { HabitsPage } from './habits/HabitsPage'
import { LoginPage } from './pages/LoginPage'
import { PrizesPage } from './prizes/PrizesPage'
import { RegisterPage } from './pages/RegisterPage'
import { TodayPage } from './today/TodayPage'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
      <Route path="/register" element={<GuestOnly><RegisterPage /></GuestOnly>} />
      <Route path="/" element={<RequireAuth><TodayPage /></RequireAuth>} />
      <Route path="/habits" element={<RequireAuth><HabitsPage /></RequireAuth>} />
      <Route path="/prizes" element={<RequireAuth><PrizesPage /></RequireAuth>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
