import { useEffect, type ReactNode } from 'react'
import { NavLink } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { markOtherScreenShown } from '../intro/firstScreen'
import { usePointsSummary } from '../points/pointsApi'
import { Logo } from './Logo'
import { Points } from './PointsIcon'
import { ThemeToggle } from './ThemeToggle'

const NAV = [
  { to: '/', label: 'Today' },
  { to: '/habits', label: 'Habits' },
  { to: '/prizes', label: 'Prizes' },
  { to: '/analytics', label: 'Stats' },
]

function NavTabs({ className }: { className: string }) {
  return (
    <nav className={`${className} text-sm`}>
      {NAV.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end
          className={({ isActive }) =>
            `px-2.5 py-1.5 rounded-lg text-center ${isActive ? 'bg-slate-800 text-slate-100' : 'text-slate-400 hover:text-slate-200'}`
          }
        >
          {item.label}
        </NavLink>
      ))}
    </nav>
  )
}

/** Page frame for logged-in screens: navigation, points balance, level and log out. */
export function AppShell({ children }: { children: ReactNode }) {
  const { logout } = useAuth()
  const summary = usePointsSummary()
  // Logging out lands on the login page; the intro is only for the first screen of a visit.
  useEffect(markOtherScreenShown, [])

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 sticky top-0 bg-slate-950/90 backdrop-blur z-10">
        <div className="max-w-3xl mx-auto px-4 h-14 flex items-center justify-between gap-3">
          <div className="flex items-center gap-4 min-w-0">
            {/* When the name doesn't fit beside the points and level badges (narrow phones, big balances), it wraps onto a
                second line that the fixed height clips, so only the mark shows. It never overlaps the badges. */}
            <Logo className="text-lg min-w-0 h-7 flex-wrap overflow-hidden" />
            <NavTabs className="hidden md:flex gap-1" />
          </div>

          <div className="flex items-center gap-1.5 sm:gap-2 text-sm shrink-0">
            {summary.data && (
              <>
                <span className="rounded-full bg-amber-400/15 text-amber-300 font-semibold px-2.5 py-1" title="Points to spend">
                  <Points value={summary.data.balance} />
                </span>
                <span className="rounded-full bg-violet-500/15 text-violet-300 font-semibold px-2.5 py-1" title="Level">
                  Lv {summary.data.level}
                </span>
              </>
            )}
            {/* On phones the top row is full, so the theme button sits next to the tabs below instead. */}
            <div className="hidden md:block">
              <ThemeToggle />
            </div>
            <button onClick={logout} className="text-slate-400 hover:text-slate-100">
              Log out
            </button>
          </div>
        </div>
        {/* Phones: tabs get their own row so they never run under the points and level badges. */}
        <div className="md:hidden flex items-center gap-1 px-4 pb-2">
          <NavTabs className="flex-1 grid grid-cols-4 gap-1" />
          {/* Negative margin: a full 44px tap area without making the row taller. */}
          <ThemeToggle className="-my-1.5" />
        </div>
      </header>
      <main className="max-w-3xl mx-auto px-4 py-8">{children}</main>
    </div>
  )
}
