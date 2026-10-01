import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import { AppShell } from '../components/AppShell'
import { api } from '../lib/api'
import { pointsKey, type Transaction } from '../points/pointsApi'
import { Heatmap } from './Heatmap'
import { PointsChart } from './PointsChart'
import { parseDay, periodLabel, shiftPeriod, useAnalytics, useHeatmap, type Analytics, type HabitStats, type Period } from './analyticsApi'
import { viz } from './vizTheme'

const PERIODS: { value: Period; label: string }[] = [
  { value: 'WEEK', label: 'Week' },
  { value: 'MONTH', label: 'Month' },
  { value: 'YEAR', label: 'Year' },
]

export function AnalyticsPage() {
  const [period, setPeriod] = useState<Period>('WEEK')
  const [date, setDate] = useState<string | null>(null) // null = the current period
  const analytics = useAnalytics(period, date)
  const a = analytics.data
  const heatmap = useHeatmap(a ? parseDay(a.start).getFullYear() : undefined)

  const isCurrent = a ? a.today >= a.start && a.today <= a.end : true
  const canGoForward = a ? shiftPeriod(a.period, a.start, 1) <= a.today : false

  function choosePeriod(next: Period) {
    setPeriod(next)
    setDate(a?.today ?? null) // keep the view around "now" when switching granularity
  }

  return (
    <AppShell>
      <h1 className="text-2xl font-bold mb-4">Analytics</h1>

      {/* Filters: one row above everything they scope. */}
      <div className="flex flex-wrap items-center gap-3 mb-6">
        <div className="flex rounded-lg bg-slate-900 border border-slate-800 p-0.5 text-sm" role="tablist" aria-label="Period">
          {PERIODS.map((p) => (
            <button
              key={p.value}
              role="tab"
              aria-selected={period === p.value}
              onClick={() => choosePeriod(p.value)}
              className={`px-3 py-1.5 rounded-md ${period === p.value ? 'bg-slate-700 text-slate-100' : 'text-slate-400 hover:text-slate-200'}`}
            >
              {p.label}
            </button>
          ))}
        </div>

        {a && (
          <div className="flex items-center gap-1 text-sm">
            <button onClick={() => setDate(shiftPeriod(a.period, a.start, -1))} aria-label="Previous period" className="size-8 rounded-lg hover:bg-slate-800 text-slate-300">
              ‹
            </button>
            <span className="min-w-36 text-center font-medium">{periodLabel(a)}</span>
            <button
              onClick={() => setDate(shiftPeriod(a.period, a.start, 1))}
              disabled={!canGoForward}
              aria-label="Next period"
              className="size-8 rounded-lg hover:bg-slate-800 text-slate-300 disabled:opacity-30 disabled:hover:bg-transparent"
            >
              ›
            </button>
            {!isCurrent && (
              <button onClick={() => setDate(null)} className="ml-1 text-emerald-400 hover:underline">
                Back to now
              </button>
            )}
          </div>
        )}
      </div>

      {analytics.isPending && <p className="text-slate-400">Loading…</p>}
      {analytics.isError && <p role="alert" className="text-red-300">Couldn't load analytics: {analytics.error.message}</p>}

      {a && (
        // While the next period loads, keep the current one visible but dimmed.
        <div className={`space-y-6 transition-opacity ${analytics.isPlaceholderData ? 'opacity-60' : ''}`}>
          <StatTiles a={a} />
          <PointsChart period={a.period} series={a.series} />
          <HabitCompletion habits={a.habits} isCurrent={isCurrent} />
          {heatmap.data && <Heatmap data={heatmap.data} />}
          <div className="grid md:grid-cols-2 gap-6">
            <TopPrizes prizes={a.topPrizes} />
            <RecentActivity />
          </div>
        </div>
      )}
    </AppShell>
  )
}

function StatTiles({ a }: { a: Analytics }) {
  const s = a.summary
  const tiles = [
    { label: 'Completion', value: s.completionRate == null ? '–' : `${s.completionRate}%`, note: `${s.activeDays} active day${s.activeDays === 1 ? '' : 's'}` },
    { label: 'Check-ins', value: s.checkIns.toLocaleString(), note: `${a.habits.length} habit${a.habits.length === 1 ? '' : 's'}` },
    { label: 'Points earned', value: s.pointsEarned.toLocaleString(), note: 'incl. streak bonuses' },
    { label: 'Points spent', value: s.pointsSpent.toLocaleString(), note: `${s.redemptions} prize${s.redemptions === 1 ? '' : 's'} redeemed` },
  ]
  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
      {tiles.map((t) => (
        <div key={t.label} className="bg-slate-900 border border-slate-800 rounded-2xl p-4">
          <p className="text-sm text-slate-400">{t.label}</p>
          <p className="text-2xl font-semibold mt-1">{t.value}</p>
          <p className="text-xs text-slate-500 mt-1">{t.note}</p>
        </div>
      ))}
    </div>
  )
}

function HabitCompletion({ habits, isCurrent }: { habits: HabitStats[]; isCurrent: boolean }) {
  return (
    <section className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
      <h2 className="font-semibold">Habit completion</h2>
      <p className="text-sm text-slate-400 mb-4">
        Check-ins done vs what each habit's target asks for{isCurrent ? ' so far' : ''}.
      </p>

      {habits.length === 0 ? (
        <p className="text-slate-400 text-sm">
          No habits in this period. <Link to="/habits" className="text-emerald-400 hover:underline">Add one</Link>.
        </p>
      ) : (
        <ul className="space-y-4">
          {habits.map((h) => {
            const unit = h.frequency === 'DAILY' ? 'day' : h.frequency === 'WEEKLY' ? 'week' : 'month'
            return (
              <li key={h.id}>
                <div className="flex items-baseline justify-between gap-3 text-sm mb-1.5">
                  <span className="truncate">
                    <span aria-hidden className="mr-1.5">{h.icon ?? '⭐'}</span>{h.name}
                    {h.archived && <span className="text-slate-500"> · archived</span>}
                  </span>
                  <span className="shrink-0 tabular-nums">
                    <span className="font-semibold">{h.completionRate == null ? '–' : `${h.completionRate}%`}</span>
                    <span className="text-slate-400"> · {h.checkIns}/{formatExpected(h.expected)}</span>
                  </span>
                </div>
                <div
                  className="h-2 rounded-full overflow-hidden"
                  style={{ background: viz.meterTrack }}
                  role="meter"
                  aria-valuenow={h.completionRate ?? 0}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-label={`${h.name} completion`}
                >
                  <div className="h-full rounded-full" style={{ width: `${h.completionRate ?? 0}%`, background: viz.meterFill }} />
                </div>
                <p className="text-xs text-slate-400 mt-1">
                  🔥 {h.currentStreak} {unit}
                  {h.currentStreak === 1 ? '' : 's'} streak · best {h.bestStreak}
                </p>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}

function TopPrizes({ prizes }: { prizes: Analytics['topPrizes'] }) {
  return (
    <section className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
      <h2 className="font-semibold mb-3">Prizes redeemed</h2>
      {prizes.length === 0 ? (
        <p className="text-sm text-slate-400">None in this period.</p>
      ) : (
        <ul className="space-y-2">
          {prizes.map((p) => (
            <li key={p.id} className="flex items-center gap-3 text-sm">
              <span className="text-xl" aria-hidden>{p.icon ?? '🎁'}</span>
              <span className="flex-1 truncate">{p.name}</span>
              <span className="text-slate-400 tabular-nums">×{p.count} · 🪙 {p.pointsSpent}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

const TYPE_LABEL: Record<Transaction['type'], string> = {
  EARN: 'Check-in',
  STREAK_BONUS: 'Streak bonus',
  UNDO: 'Undo',
  REDEEM: 'Redeemed',
}

/** The latest ledger rows, so every point change can be traced. */
function RecentActivity() {
  const history = useQuery({
    queryKey: [...pointsKey, 'history', 10],
    queryFn: () => api<Transaction[]>('/points/history?limit=10'),
  })
  return (
    <section className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
      <h2 className="font-semibold mb-3">Recent activity</h2>
      {history.data?.length === 0 && <p className="text-sm text-slate-400">No points yet.</p>}
      <ul className="space-y-2">
        {history.data?.map((t) => (
          <li key={t.id} className="flex items-center gap-3 text-sm">
            <span className={`w-12 text-right font-semibold tabular-nums ${t.amount > 0 ? 'text-emerald-300' : 'text-slate-300'}`}>
              {t.amount > 0 ? `+${t.amount}` : t.amount}
            </span>
            <span className="flex-1 truncate">{t.description}</span>
            <span className="text-xs text-slate-500 shrink-0" title={TYPE_LABEL[t.type]}>
              {new Date(t.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}

function formatExpected(expected: number): string {
  return Number.isInteger(expected) ? String(expected) : expected.toFixed(1)
}
