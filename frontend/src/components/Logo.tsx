import { useId } from 'react'

/**
 * The HabitQuest mark: a green Q whose tail is a gold check mark (a quest, completed).
 * Same artwork as public/favicon.svg. The ring has a gap where the check crosses it, so the two
 * shapes stay apart even at 16px. useId keeps gradient/mask ids unique when two logos share a page.
 */
export function LogoMark({ className = 'size-8' }: { className?: string }) {
  const id = useId()
  const ring = `${id}-ring`
  const check = `${id}-check`
  const gap = `${id}-gap`
  const tick = 'M29 42 L38 51 L56 28'

  return (
    <svg viewBox="0 0 64 64" aria-hidden className={`shrink-0 ${className}`}>
      <defs>
        <linearGradient id={ring} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#34d399" />
          <stop offset="1" stopColor="#059669" />
        </linearGradient>
        <linearGradient id={check} x1="0" y1="1" x2="1" y2="0">
          <stop offset="0" stopColor="#f59e0b" />
          <stop offset="1" stopColor="#fcd34d" />
        </linearGradient>
        <mask id={gap} maskUnits="userSpaceOnUse" x="0" y="0" width="64" height="64">
          <rect width="64" height="64" fill="#fff" />
          <path d={tick} fill="none" stroke="#000" strokeWidth="14" strokeLinecap="round" strokeLinejoin="round" />
        </mask>
      </defs>
      <circle cx="26" cy="30" r="18" fill="none" stroke={`url(#${ring})`} strokeWidth="8" mask={`url(#${gap})`} />
      <path d={tick} fill="none" stroke={`url(#${check})`} strokeWidth="8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

/** The mark plus the "HabitQuest" name. The name is real text, so screen readers read it. */
export function Logo({ className = '', markClassName = 'size-7' }: { className?: string; markClassName?: string }) {
  return (
    <span className={`inline-flex items-center gap-2 font-extrabold tracking-tight ${className}`}>
      <LogoMark className={markClassName} />
      <span>
        <span className="text-slate-100">Habit</span>
        <span className="text-emerald-400">Quest</span>
      </span>
    </span>
  )
}
