import type { Frequency } from '../habits/habitsApi'
import { streakTier, streakUnit } from './streak'
import { useTheme } from './theme'

type FlameProps = { flame: string; core: string; edge: string; glow?: boolean; className?: string }

/** A flame whose color shows how long the streak is; the number and tooltip carry the meaning too. */
export function StreakFlame({ flame, core, edge, glow = false, className = '' }: FlameProps) {
  // On white, the bright flames (gold, orange) fade into the page, so light mode outlines them in a darker shade.
  const outline = useTheme() === 'light' ? { stroke: edge, strokeWidth: 1.25, strokeLinejoin: 'round' as const } : {}
  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden
      className={`inline-block size-[1.35em] shrink-0 align-[-0.25em] ${className}`}
      style={glow ? { filter: `drop-shadow(0 0 3px ${flame})` } : undefined}
    >
      <path
        d="M12 1.5c.7 3.1-1.1 5.1-3 7.1C6.9 10.8 4.8 13 4.8 16.2a7.2 7.2 0 0 0 14.4 0c0-3.1-1.5-5.4-3.1-7.1.2 1.8-.5 3.2-1.9 3.8.5-4.4-.6-8.2-2.2-11.4z"
        fill={flame}
        {...outline}
      />
      <path
        d="M12 22.4a3.7 3.7 0 0 1-3.7-3.7c0-2.1 1.2-3.3 2.6-4.7.2 1.2 1 2.1 2.1 2.4-.1-1.2.3-2.4 1-3.2 1.1 1.4 1.8 3 1.8 5.1a3.7 3.7 0 0 1-3.8 4.1z"
        fill={core}
      />
    </svg>
  )
}

type Props = { frequency: Frequency; streak: number; best?: number; className?: string }

/** "🔥 7" for the Today list. Renders nothing until the streak is long enough to count (3 days / 2 periods). */
export function StreakBadge({ frequency, streak, best, className = '' }: Props) {
  const tier = streakTier(frequency, streak)
  if (!tier.visible) return null
  const label = `${streak}-${streakUnit(frequency, 1)} streak · ${tier.name}${best != null ? ` · best ${best}` : ''}`
  return (
    <span className={`inline-flex items-center gap-1 font-semibold tabular-nums ${className}`} title={label}>
      <StreakFlame flame={tier.flame} core={tier.core} edge={tier.edge} glow={tier.glow} />
      {streak}
      <span className="sr-only"> {label}</span>
    </span>
  )
}
