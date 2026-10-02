import type { Frequency } from '../habits/habitsApi'

export type StreakTier = {
  /** false while the streak is too short to show (TikTok-style: a streak only "starts" after a few in a row) */
  visible: boolean
  name: string
  flame: string
  core: string
  /** the top tiers (30+ days) get a soft glow, so long streaks stand out */
  glow: boolean
}

// Flame colors heat up as the streak grows, like TikTok's streak flame.
// The 7- and 30-day tiers line up with the +2 and +4 streak bonus tiers (PRD §5.4).
const TIERS = {
  spark: { name: 'Spark', flame: '#f97316', core: '#fed7aa' }, // orange
  hot: { name: 'Hot', flame: '#ef4444', core: '#fecaca' }, // red, +2 bonus
  blazing: { name: 'Blazing', flame: '#d946ef', core: '#f5d0fe' }, // magenta, +4 bonus
  legendary: { name: 'Legendary', flame: '#3b82f6', core: '#bfdbfe' }, // blue fire
  mythic: { name: 'Mythic', flame: '#facc15', core: '#fef9c3' }, // gold
} as const

// Thresholds per frequency: [visible from, hot, blazing, legendary, mythic].
// Daily counts days; weekly/monthly count periods in a row where the target was met.
const THRESHOLDS: Record<Frequency, [number, number, number, number, number]> = {
  DAILY: [3, 7, 30, 100, 365],
  WEEKLY: [2, 3, 8, 26, 52],
  MONTHLY: [2, 3, 8, 12, 24],
}

export function streakTier(frequency: Frequency, streak: number): StreakTier {
  const [start, hot, blazing, legendary, mythic] = THRESHOLDS[frequency]
  const tier =
    streak >= mythic ? TIERS.mythic
      : streak >= legendary ? TIERS.legendary
        : streak >= blazing ? TIERS.blazing
          : streak >= hot ? TIERS.hot
            : TIERS.spark
  return { visible: streak >= start, glow: streak >= blazing, ...tier }
}

export function streakUnit(frequency: Frequency, count: number): string {
  const unit = frequency === 'DAILY' ? 'day' : frequency === 'WEEKLY' ? 'week' : 'month'
  return count === 1 ? unit : `${unit}s`
}
