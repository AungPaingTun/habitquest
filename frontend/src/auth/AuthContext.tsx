import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, getToken, setToken } from '../lib/api'

export type User = {
  id: number
  email: string
  displayName: string
  timezone: string
  createdAt: string
}

type AuthResponse = { token: string; user: User }

type AuthContextValue = {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, displayName: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  // True while we check a saved token on page load, so protected pages don't flash the login screen.
  const [loading, setLoading] = useState(() => getToken() !== null)

  useEffect(() => {
    if (!getToken()) return
    api<User>('/auth/me')
      .then(setUser)
      .catch(() => setToken(null)) // expired or invalid token → treat as logged out
      .finally(() => setLoading(false))
  }, [])

  const handleAuth = useCallback((response: AuthResponse) => {
    setToken(response.token)
    setUser(response.user)
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    handleAuth(await api<AuthResponse>('/auth/login', { method: 'POST', body: { email, password } }))
  }, [handleAuth])

  const register = useCallback(async (email: string, password: string, displayName: string) => {
    // The browser knows the user's time zone (e.g. "Asia/Yangon"); the backend uses it to decide what "today" is.
    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
    handleAuth(await api<AuthResponse>('/auth/register', {
      method: 'POST',
      body: { email, password, displayName, timezone },
    }))
  }, [handleAuth])

  const logout = useCallback(() => {
    setToken(null)
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

// The hook lives next to its provider on purpose; this only affects hot-reload granularity in dev.
// oxlint-disable-next-line react/only-export-components
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
