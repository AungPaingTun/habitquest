import type { ReactNode } from 'react'
import { useAuth } from '../auth/AuthContext'

/** Page frame for logged-in screens: top bar with the user's name and log out. */
export function AppShell({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800">
        <div className="max-w-3xl mx-auto px-4 h-14 flex items-center justify-between">
          <span className="text-emerald-400 font-semibold">HabitQuest</span>
          <div className="flex items-center gap-4 text-sm">
            <span className="text-slate-400 hidden sm:inline">{user?.displayName}</span>
            <button onClick={logout} className="text-slate-400 hover:text-slate-100">
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="max-w-3xl mx-auto px-4 py-8">{children}</main>
    </div>
  )
}
