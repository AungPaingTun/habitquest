import { useState } from 'react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { bucketLabel, bucketTitle, type Bucket, type Period } from './analyticsApi'
import { niceTicks, viz } from './vizTheme'

type Props = { period: Period; series: Bucket[] }

const SERIES = [
  { key: 'earned', label: 'Earned', color: viz.earned },
  { key: 'spent', label: 'Spent', color: viz.spent },
] as const

/** Points earned vs spent per day (week/month) or per month (year). One axis: both are points. */
export function PointsChart({ period, series }: Props) {
  const [showTable, setShowTable] = useState(false)
  const data = series.map((b) => ({ ...b, label: bucketLabel(period, b.key) }))
  const ticks = niceTicks(Math.max(0, ...series.map((b) => Math.max(b.earned, b.spent))))

  return (
    <section className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <h2 className="font-semibold">Points earned vs spent</h2>
        <div className="flex items-center gap-4 text-sm">
          {/* Legend: always shown for 2 series; swatches are rects because the marks are bars. */}
          <ul className="flex gap-4 text-slate-300" aria-label="Legend">
            {SERIES.map((s) => (
              <li key={s.key} className="flex items-center gap-1.5">
                <span className="size-2.5 rounded-sm" style={{ background: s.color }} aria-hidden />
                {s.label}
              </li>
            ))}
          </ul>
          <button onClick={() => setShowTable((v) => !v)} className="text-slate-400 hover:text-slate-200 underline-offset-2 hover:underline">
            {showTable ? 'Show chart' : 'Show table'}
          </button>
        </div>
      </div>

      {showTable ? (
        <div className="max-h-72 overflow-y-auto pr-2">
          <table className="w-full text-sm">
            <thead className="text-slate-400 text-left sticky top-0 bg-slate-900">
              <tr>
                <th className="font-medium py-1.5">{period === 'YEAR' ? 'Month' : 'Day'}</th>
                <th className="font-medium py-1.5 text-right">Earned</th>
                <th className="font-medium py-1.5 text-right">Spent</th>
                <th className="font-medium py-1.5 text-right">Check-ins</th>
              </tr>
            </thead>
            <tbody className="tabular-nums">
              {series.filter((b) => !b.future).map((b) => (
                <tr key={b.key} className="border-t border-slate-800">
                  <td className="py-1.5 text-slate-300">{bucketTitle(period, b.key)}</td>
                  <td className="py-1.5 text-right">{b.earned}</td>
                  <td className="py-1.5 text-right">{b.spent}</td>
                  <td className="py-1.5 text-right">{b.checkIns}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="h-64">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} barGap={2} barCategoryGap={period === 'MONTH' ? '20%' : '30%'} margin={{ top: 4, right: 4, bottom: 0, left: -12 }}>
              <CartesianGrid vertical={false} stroke={viz.grid} />
              <XAxis
                dataKey="label"
                tickLine={false}
                axisLine={{ stroke: viz.axis }}
                tick={{ fill: viz.muted, fontSize: 12 }}
                interval={period === 'MONTH' ? 4 : 0}
              />
              <YAxis ticks={ticks} domain={[0, ticks[ticks.length - 1]]} allowDecimals={false} tickLine={false} axisLine={false} tick={{ fill: viz.muted, fontSize: 12 }} width={44} />
              <Tooltip cursor={{ fill: 'rgba(148,163,184,0.08)' }} content={<PointsTooltip period={period} />} />
              {SERIES.map((s) => (
                <Bar key={s.key} dataKey={s.key} name={s.label} fill={s.color} maxBarSize={24} radius={[4, 4, 0, 0]} isAnimationActive={false} />
              ))}
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </section>
  )
}

type TooltipProps = {
  period: Period
  active?: boolean
  payload?: { payload: Bucket }[]
}

/** Values lead, labels follow; series keyed with a short line, not a box. */
function PointsTooltip({ period, active, payload }: TooltipProps) {
  if (!active || !payload?.length) return null
  const bucket = payload[0].payload
  return (
    <div className="rounded-lg bg-slate-950 border border-slate-700 px-3 py-2 text-sm shadow-lg">
      <p className="text-slate-400 mb-1">{bucketTitle(period, bucket.key)}</p>
      {SERIES.map((s) => (
        <p key={s.key} className="flex items-center gap-2">
          <span className="w-3 h-0.5 rounded" style={{ background: s.color }} aria-hidden />
          <span className="font-semibold text-slate-100">{bucket[s.key]}</span>
          <span className="text-slate-400">{s.label.toLowerCase()}</span>
        </p>
      ))}
      <p className="text-slate-400 mt-1">{bucket.checkIns} check-ins</p>
    </div>
  )
}
