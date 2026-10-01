// Chart colors, validated with the data-viz palette checker against the card surface (#0f172a, dark):
// - series: categorical slots 1–2 (blue, orange), colorblind-safe as a pair, ≥3:1 contrast on the surface
// - heat: one-hue blue ramp, evenly stepped, darkest step still ≥2:1 against the surface
// Text never uses these colors; labels and values use the slate text colors.

export const viz = {
  surface: '#0f172a', // slate-900, the card background
  grid: '#1e293b', // slate-800, hairline gridlines
  axis: '#334155', // slate-700, baseline
  muted: '#94a3b8', // slate-400, axis labels
  earned: '#3987e5',
  spent: '#d95926',
  meterFill: '#3987e5',
  meterTrack: '#1e293b',
  heatEmpty: '#1e293b',
  heat: ['#184f95', '#256abf', '#3987e5', '#6da7ec', '#9ec5f4'],
} as const

/** Clean y-axis ticks (0, 50, 100 …) with roughly {@code count} steps, using 1/2/2.5/5 × 10ⁿ step sizes. */
export function niceTicks(max: number, count = 4): number[] {
  if (max <= 0) return [0, 5, 10]
  const raw = max / count
  const magnitude = 10 ** Math.floor(Math.log10(raw))
  const normalized = raw / magnitude
  let factor = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 2.5 ? 2.5 : normalized <= 5 ? 5 : 10
  if (factor === 2.5 && magnitude < 10) factor = 5 // points are whole numbers: 2.5 steps would round to 3s
  const step = Math.max(1, Math.ceil(factor * magnitude))
  const top = Math.ceil(max / step) * step
  return Array.from({ length: top / step + 1 }, (_, i) => i * step)
}

/** Maps a day's count to a heat step (0 = empty). Scales to the busiest day so any history has contrast. */
export function heatColor(count: number, max: number): string {
  if (count <= 0 || max <= 0) return viz.heatEmpty
  const step = Math.min(viz.heat.length - 1, Math.floor(((count - 1) / Math.max(1, max - 1)) * (viz.heat.length - 1)))
  return viz.heat[max === 1 ? viz.heat.length - 1 : step]
}
