import { Link } from 'react-router'
import { AppShell } from '../components/AppShell'
import { Points } from '../components/PointsIcon'
import { StreakBadge } from '../components/StreakBadge'
import { Toast } from '../components/Toast'
import { useToast } from '../components/useToast'
import { describeFrequency, formatDate } from '../habits/habitsApi'
import { ApiError } from '../lib/api'
import { levelProgress, type PointsSummary } from '../points/pointsApi'
import { useCheckIn, useToday, useUndoCheckIn, type TodayHabit } from './todayApi'

export function TodayPage() {
  const today = useToday()
  const checkIn = useCheckIn()
  const undo = useUndoCheckIn()
  const { toast, show } = useToast()
  const busy = checkIn.isPending || undo.isPending

  async function handleCheckIn(habit: TodayHabit) {
    const levelBefore = today.data?.points.level ?? 0
    try {
      const result = await checkIn.mutateAsync(habit.id)
      if (result.points.level > levelBefore) {
        show({ tone: 'celebrate', text: `🎉 Level up! You're now level ${result.points.level}` })
      } else if (result.bonus > 0) {
        show({ tone: 'celebrate', text: `+${result.pointsChange} pts · 🔥 ${result.currentStreak} streak bonus +${result.bonus}!` })
      } else {
        show({ tone: 'success', text: `+${result.pointsChange} pts · ${habit.name}` })
      }
    } catch (e) {
      show({ tone: 'error', text: e instanceof ApiError ? e.message : 'Check-in failed' })
    }
  }

  async function handleUndo(habit: TodayHabit) {
    try {
      const result = await undo.mutateAsync(habit.id)
      show({ tone: 'success', text: `Undone · ${result.pointsChange} pts` })
    } catch (e) {
      show({ tone: 'error', text: e instanceof ApiError ? e.message : 'Undo failed' })
    }
  }

  const habits = today.data?.habits ?? []
  const active = habits
    .filter((h) => h.status === 'ACTIVE')
    // Things still to do first, then what's finished.
    .sort((a, b) => Number(!a.canCheckIn) - Number(!b.canCheckIn))
  const upcoming = habits.filter((h) => h.status === 'UPCOMING')
  const completed = habits.filter((h) => h.status === 'COMPLETED')
  const doneCount = active.filter((h) => !h.canCheckIn).length

  return (
    <AppShell>
      {today.isPending && <p className="text-slate-400">Loading…</p>}
      {today.isError && <p role="alert" className="text-red-300">Couldn't load today: {today.error.message}</p>}

      {today.data && (
        <>
          <div className="mb-6">
            <p className="text-slate-400 text-sm">{formatLongDate(today.data.date)}</p>
            <h1 className="text-2xl font-bold">
              Today {active.length > 0 && <span className="text-slate-500 font-normal">· {doneCount}/{active.length} done</span>}
            </h1>
          </div>

          <LevelCard points={today.data.points} />

          {active.length === 0 ? (
            <div className="text-center border border-dashed border-slate-800 rounded-2xl py-12 px-6">
              <p className="text-4xl mb-3">🌱</p>
              <p className="font-medium">Nothing to do today</p>
              <p className="text-slate-400 text-sm mt-1">
                <Link to="/habits" className="text-emerald-400 hover:underline">Add a habit</Link> to start earning points.
              </p>
            </div>
          ) : (
            <ul className="space-y-3">
              {active.map((habit) => (
                <HabitRow key={habit.id} habit={habit} busy={busy} onCheckIn={handleCheckIn} onUndo={handleUndo} />
              ))}
            </ul>
          )}

          {upcoming.length > 0 && (
            <Section title="Upcoming">
              {upcoming.map((h) => (
                <MutedRow key={h.id} habit={h} note={`Starts ${formatDate(h.startDate)}`} />
              ))}
            </Section>
          )}

          {completed.length > 0 && (
            <Section title="Completed">
              {completed.map((h) => (
                <MutedRow key={h.id} habit={h} note={`Ended ${formatDate(h.endDate!)} · best streak ${h.bestStreak}`} />
              ))}
            </Section>
          )}
        </>
      )}

      <Toast toast={toast} />
    </AppShell>
  )
}

function LevelCard({ points }: { points: PointsSummary }) {
  const progress = levelProgress(points)
  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 mb-6">
      <div className="flex items-end justify-between gap-4">
        <div>
          <p className="text-sm text-slate-400">Level</p>
          <p className="text-3xl font-bold text-violet-300">{points.level}</p>
        </div>
        <div className="text-right">
          <p className="text-sm text-slate-400">Points to spend</p>
          <p className="text-3xl font-bold text-amber-300"><Points value={points.balance} /></p>
        </div>
      </div>
      <div className="mt-4">
        <div className="h-2 rounded-full bg-slate-800 overflow-hidden" role="progressbar" aria-valuenow={progress} aria-valuemin={0} aria-valuemax={100} aria-label="Progress to next level">
          <div className="h-full bg-violet-400 transition-all" style={{ width: `${progress}%` }} />
        </div>
        <p className="text-xs text-slate-400 mt-2">
          {points.lifetimeXp} XP · {points.nextLevelXp - points.lifetimeXp} XP to level {points.level + 1}
        </p>
      </div>
    </div>
  )
}

type RowProps = {
  habit: TodayHabit
  busy: boolean
  onCheckIn: (habit: TodayHabit) => void
  onUndo: (habit: TodayHabit) => void
}

function HabitRow({ habit, busy, onCheckIn, onUndo }: RowProps) {
  const periodic = habit.frequency !== 'DAILY'
  const targetMet = habit.periodCount >= habit.targetCount
  const period = habit.frequency === 'WEEKLY' ? 'week' : 'month'

  return (
    <li className={`flex items-center gap-4 rounded-2xl border p-4 transition-colors ${
      habit.canCheckIn ? 'bg-slate-900 border-slate-800' : 'bg-slate-900/50 border-slate-800/60'
    }`}>
      <button
        onClick={() => onCheckIn(habit)}
        disabled={!habit.canCheckIn || busy}
        aria-label={habit.doneToday ? `${habit.name} done today` : `Check in ${habit.name}`}
        className={`size-11 shrink-0 rounded-full border-2 grid place-items-center text-lg font-bold transition-colors ${
          habit.doneToday || targetMet
            ? 'bg-emerald-500 border-emerald-500 text-slate-950'
            : 'border-slate-600 hover:border-emerald-400 hover:bg-emerald-500/10 disabled:opacity-50'
        }`}
      >
        {(habit.doneToday || targetMet) && '✓'}
      </button>

      <span className="text-2xl shrink-0" aria-hidden>{habit.icon ?? '⭐'}</span>

      <div className="min-w-0 flex-1">
        <p className={`font-medium truncate ${habit.doneToday ? 'text-slate-400 line-through decoration-slate-600' : ''}`}>
          {habit.name}
        </p>
        <p className="text-sm text-slate-400">
          {periodic ? `${habit.periodCount}/${habit.targetCount} this ${period}` : describeFrequency(habit)}
          {targetMet && periodic && !habit.doneToday && ' · target reached 🎯'}
        </p>
      </div>

      <StreakBadge frequency={habit.frequency} streak={habit.currentStreak} best={habit.bestStreak} className="shrink-0 text-sm text-slate-200" />

      <div className="shrink-0 w-16 text-right">
        {habit.doneToday ? (
          <button onClick={() => onUndo(habit)} disabled={busy} className="text-sm text-slate-400 hover:text-slate-100 disabled:opacity-50">
            Undo
          </button>
        ) : (
          <span className="text-sm font-semibold text-emerald-300">+{habit.points}</span>
        )}
      </div>
    </li>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-8">
      <h2 className="text-sm font-semibold text-slate-400 mb-3">{title}</h2>
      <ul className="space-y-2">{children}</ul>
    </section>
  )
}

function MutedRow({ habit, note }: { habit: TodayHabit; note: string }) {
  return (
    <li className="flex items-center gap-3 rounded-xl border border-slate-800/60 px-4 py-3 text-slate-400">
      <span aria-hidden>{habit.icon ?? '⭐'}</span>
      <span className="flex-1 truncate">{habit.name}</span>
      <span className="text-xs">{note}</span>
    </li>
  )
}

/** "Friday, October 2" from "2026-10-02". */
function formatLongDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(year, month - 1, day).toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })
}
