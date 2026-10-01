import { useMemo, useState } from 'react'
import { parseDay, toIso, type Heatmap as HeatmapData } from './analyticsApi'
import { heatColor, viz } from './vizTheme'

const CELL = 10
const GAP = 2
const DAY_LABELS = ['Mon', '', 'Wed', '', 'Fri', '', '']

/** GitHub-style year grid: one column per week (Mon–Sun), one cell per day, darker blue = fewer check-ins. */
export function Heatmap({ data }: { data: HeatmapData }) {
  const [hover, setHover] = useState<{ date: string; count: number; x: number; y: number } | null>(null)

  const { weeks, monthLabels, activeDays } = useMemo(() => {
    const counts = new Map(data.days.map((d) => [d.date, d.count]))
    const jan1 = new Date(data.year, 0, 1)
    // Start on the Monday on or before Jan 1 so every column is a full Mon–Sun week.
    const first = new Date(jan1)
    first.setDate(first.getDate() - ((jan1.getDay() + 6) % 7))

    const weeks: { date: string; count: number; inYear: boolean; future: boolean }[][] = []
    const monthLabels: { col: number; label: string }[] = []
    const cursor = new Date(first)
    while (cursor.getFullYear() <= data.year) {
      const week = []
      for (let i = 0; i < 7; i++) {
        const iso = toIso(cursor)
        const inYear = cursor.getFullYear() === data.year
        if (inYear && cursor.getDate() === 1) {
          monthLabels.push({ col: weeks.length, label: cursor.toLocaleDateString(undefined, { month: 'short' }) })
        }
        week.push({ date: iso, count: counts.get(iso) ?? 0, inYear, future: iso > data.today })
        cursor.setDate(cursor.getDate() + 1)
      }
      weeks.push(week)
      if (cursor.getFullYear() > data.year) break
    }
    return { weeks, monthLabels, activeDays: data.days.length }
  }, [data])

  const width = weeks.length * (CELL + GAP)
  const height = 7 * (CELL + GAP)
  const totalCheckIns = data.days.reduce((sum, d) => sum + d.count, 0)

  return (
    <section className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
      <div className="flex flex-wrap items-baseline justify-between gap-2 mb-4">
        <h2 className="font-semibold">{data.year} at a glance</h2>
        <p className="text-sm text-slate-400">
          {totalCheckIns} check-ins on {activeDays} days{data.maxCount > 0 && ` · busiest day ${data.maxCount}`}
        </p>
      </div>

      <div className="overflow-x-auto relative [color-scheme:dark]" onMouseLeave={() => setHover(null)}>
        <svg
          width={width + 30}
          height={height + 18}
          role="img"
          aria-label={`Check-ins per day in ${data.year}: ${totalCheckIns} check-ins on ${activeDays} days.`}
        >
          {monthLabels.map((m) => (
            <text key={m.label} x={30 + m.col * (CELL + GAP)} y={10} fill={viz.muted} fontSize={10}>{m.label}</text>
          ))}
          {DAY_LABELS.map((label, i) =>
            label ? <text key={label} x={0} y={18 + i * (CELL + GAP) + CELL - 2} fill={viz.muted} fontSize={10}>{label}</text> : null,
          )}
          <g transform="translate(30, 16)">
            {weeks.map((week, col) =>
              week.map((day, row) =>
                day.inYear && !day.future ? (
                  <rect
                    key={day.date}
                    x={col * (CELL + GAP)}
                    y={row * (CELL + GAP)}
                    width={CELL}
                    height={CELL}
                    rx={2}
                    fill={heatColor(day.count, data.maxCount)}
                    onMouseEnter={() => setHover({ ...day, x: 30 + col * (CELL + GAP), y: 16 + row * (CELL + GAP) })}
                  >
                    <title>{`${day.count} check-in${day.count === 1 ? '' : 's'} · ${formatDay(day.date)}`}</title>
                  </rect>
                ) : null,
              ),
            )}
          </g>
        </svg>

        {hover && (
          <div
            className="pointer-events-none absolute rounded-lg bg-slate-950 border border-slate-700 px-2.5 py-1.5 text-xs shadow-lg whitespace-nowrap"
            style={{ left: hover.x, top: hover.y - 36 }}
          >
            <span className="font-semibold text-slate-100">{hover.count}</span>{' '}
            <span className="text-slate-400">check-in{hover.count === 1 ? '' : 's'} · {formatDay(hover.date)}</span>
          </div>
        )}
      </div>

      <div className="flex items-center justify-end gap-1.5 mt-3 text-xs text-slate-400">
        Less
        {[viz.heatEmpty, ...viz.heat].map((color) => (
          <span key={color} className="size-[10px] rounded-[2px]"style={{ background: color }} aria-hidden />
        ))}
        More
      </div>
    </section>
  )
}

function formatDay(iso: string): string {
  return parseDay(iso).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })
}
