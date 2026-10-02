/**
 * The HabitQuest points coin: gold coin with an embossed check mark (a completed habit).
 * Taken from the designed preview sheet (64×64 artwork), drawn inline so it's crisp at any size.
 * Sized in em, so it scales with the text it sits next to.
 */
export function PointsIcon({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" aria-hidden className={`inline-block size-[1.1em] shrink-0 align-[-0.15em] ${className}`}>
      <circle cx="32" cy="32" r="28" fill="#fbbf24" stroke="#b45309" strokeWidth="4" />
      <circle cx="32" cy="32" r="19" fill="#f59e0b" />
      <path d="M12.1 20.5 A23 23 0 0 1 20.5 12.1" fill="none" stroke="#fef3c7" strokeWidth="3" strokeLinecap="round" opacity="0.9" />
      <path d="M22 33 L29 40 L42 25" fill="none" stroke="#b45309" strokeWidth="6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

/** A points amount with the coin in front, e.g. [coin] 1,250. The number is the accessible text. */
export function Points({ value, className = '' }: { value: number; className?: string }) {
  return (
    <span className={`inline-flex items-center gap-1 ${className}`}>
      <PointsIcon />
      <span>
        {value.toLocaleString()}
        <span className="sr-only"> points</span>
      </span>
    </span>
  )
}
