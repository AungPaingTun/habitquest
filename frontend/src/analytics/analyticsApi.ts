import { keepPreviousData, useQuery } from '@tanstack/react-query'
import type { Frequency } from '../habits/habitsApi'
import { api } from '../lib/api'

export type Period = 'WEEK' | 'MONTH' | 'YEAR'

export type HabitStats = {
  id: number
  name: string
  icon: string | null
  frequency: Frequency
  targetCount: number
  archived: boolean
  checkIns: number
  expected: number
  completionRate: number | null
  currentStreak: number
  bestStreak: number
}

export type Bucket = { key: string; earned: number; spent: number; checkIns: number; future: boolean }

export type Analytics = {
  period: Period
  start: string
  end: string
  today: string
  summary: {
    checkIns: number
    completionRate: number | null
    pointsEarned: number
    pointsSpent: number
    redemptions: number
    activeDays: number
  }
  habits: HabitStats[]
  series: Bucket[]
  topPrizes: { id: number; name: string; icon: string | null; count: number; pointsSpent: number }[]
}

export type Heatmap = { year: number; today: string; maxCount: number; days: { date: string; count: number }[] }

/** Check-ins and redeems change every number here, so those mutations invalidate this key too. */
export const analyticsKey = ['analytics'] as const

export function useAnalytics(period: Period, date: string | null) {
  return useQuery({
    queryKey: [...analyticsKey, period, date],
    queryFn: () => api<Analytics>(`/analytics?period=${period}${date ? `&date=${date}` : ''}`),
    // Keep showing the previous period while the next one loads (no flash, no layout jump).
    placeholderData: keepPreviousData,
  })
}

export function useHeatmap(year: number | undefined) {
  return useQuery({
    queryKey: [...analyticsKey, 'heatmap', year],
    queryFn: () => api<Heatmap>(`/analytics/heatmap${year ? `?year=${year}` : ''}`),
    placeholderData: keepPreviousData,
  })
}

// ---- date helpers for "YYYY-MM-DD" strings (local, no time-zone shifts) ----

export function parseDay(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function toIso(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** The first day of the previous (-1) or next (+1) period. */
export function shiftPeriod(period: Period, start: string, direction: -1 | 1): string {
  const d = parseDay(start)
  if (period === 'WEEK') d.setDate(d.getDate() + 7 * direction)
  else if (period === 'MONTH') d.setMonth(d.getMonth() + direction, 1)
  else d.setFullYear(d.getFullYear() + direction, 0, 1)
  return toIso(d)
}

export function periodLabel(a: Pick<Analytics, 'period' | 'start' | 'end'>): string {
  const start = parseDay(a.start)
  const end = parseDay(a.end)
  if (a.period === 'YEAR') return String(start.getFullYear())
  if (a.period === 'MONTH') return start.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })
  const sameMonth = start.getMonth() === end.getMonth()
  const from = start.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
  const to = end.toLocaleDateString(undefined, sameMonth ? { day: 'numeric', year: 'numeric' } : { month: 'short', day: 'numeric', year: 'numeric' })
  return `${from} – ${to}`
}

/** Short x-axis label for a bucket key. */
export function bucketLabel(period: Period, key: string): string {
  if (period === 'YEAR') return parseDay(`${key}-01`).toLocaleDateString(undefined, { month: 'short' })
  const d = parseDay(key)
  return period === 'WEEK' ? d.toLocaleDateString(undefined, { weekday: 'short' }) : String(d.getDate())
}

/** Long label for tooltips and the table view. */
export function bucketTitle(period: Period, key: string): string {
  if (period === 'YEAR') return parseDay(`${key}-01`).toLocaleDateString(undefined, { month: 'long', year: 'numeric' })
  return parseDay(key).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })
}
