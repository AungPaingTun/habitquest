import { setTheme, useTheme } from './theme'

/** Round sun/moon button that switches between light and dark colors. */
export function ThemeToggle({ className = '' }: { className?: string }) {
  const theme = useTheme()
  const next = theme === 'dark' ? 'light' : 'dark'
  const label = `Switch to ${next} mode`

  return (
    <button
      type="button"
      onClick={(e) => {
        const r = e.currentTarget.getBoundingClientRect()
        setTheme(next, { x: r.left + r.width / 2, y: r.top + r.height / 2 })
      }}
      aria-label={label}
      title={label}
      className={`grid place-items-center size-11 shrink-0 rounded-full text-slate-400 hover:text-slate-100 hover:bg-slate-800 `
        + `focus-visible:outline-2 focus-visible:outline-emerald-500 ${className}`}
    >
      {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
    </button>
  )
}

function SunIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden className="size-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  )
}

function MoonIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden className="size-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round">
      <path d="M20.5 14.5A8.5 8.5 0 0 1 9.5 3.5a8.5 8.5 0 1 0 11 11z" />
    </svg>
  )
}
