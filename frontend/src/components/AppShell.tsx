import type { ReactNode } from 'react'
import { NavLink } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { usePointsSummary } from '../points/pointsApi'

const NAV = [
  { to: '/', label: 'Today' },
  { to: '/habits', label: 'Habits' },
  { to: '/prizes', label: 'Prizes' },
  { to: '/analytics', label: 'Stats' },
]

/** Page frame for logged-in screens: navigation, points balance, level and log out. */
export function AppShell({ children }: { children: ReactNode }) {
  const { logout } = useAuth()
  const summary = usePointsSummary()

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 sticky top-0 bg-slate-950/90 backdrop-blur z-10">
        <div className="max-w-3xl mx-auto px-4 h-14 flex items-center justify-between gap-3">
          <div className="flex items-center gap-1 sm:gap-4 min-w-0">
            <span className="text-emerald-400 font-semibold hidden sm:inline">HabitQuest</span>
            <nav className="flex gap-1 text-sm">
              {NAV.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end
                  className={({ isActive }) =>
                    `px-2.5 py-1.5 rounded-lg ${isActive ? 'bg-slate-800 text-slate-100' : 'text-slate-400 hover:text-slate-200'}`
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>

          <div className="flex items-center gap-2 text-sm shrink-0">
            {summary.data && (
              <>
                <span className="rounded-full bg-amber-400/15 text-amber-300 font-semibold px-2.5 py-1" title="Points to spend">
                  🪙 {summary.data.balance}
                </span>
                <span className="rounded-full bg-violet-500/15 text-violet-300 font-semibold px-2.5 py-1" title="Level">
                  Lv {summary.data.level}
                </span>
              </>
            )}
            <button onClick={logout} className="text-slate-400 hover:text-slate-100 ml-1">
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="max-w-3xl mx-auto px-4 py-8">{children}</main>
    </div>
  )
}
