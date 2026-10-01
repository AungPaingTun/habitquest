import { useAuth } from '../auth/AuthContext'

/** Placeholder home screen. The habit list replaces this in Step 4. */
export function HomePage() {
  const { user, logout } = useAuth()

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between">
          <span className="text-emerald-400 font-semibold">HabitQuest</span>
          <button onClick={logout} className="text-sm text-slate-400 hover:text-slate-100">
            Log out
          </button>
        </div>
      </header>

      <section className="max-w-4xl mx-auto px-4 py-16 text-center space-y-3">
        <h1 className="text-3xl font-bold">Hi, {user?.displayName} 👋</h1>
        <p className="text-slate-400">You're logged in. Your habits will appear here soon.</p>
        <p className="text-xs text-slate-500">Time zone: {user?.timezone}</p>
      </section>
    </main>
  )
}
