import { useEffect, useState } from 'react'
import { LogoMark } from './Logo'

// Fast loads finish before the message shows, so the screen doesn't flash.
const SHOW_AFTER_MS = 400
// The API runs on a Heroku dyno that sleeps when idle; waking it takes 10-15 s.
const SLOW_AFTER_MS = 4000

/** Full-page placeholder while the saved login is being checked. */
export function LoadingScreen() {
  const [phase, setPhase] = useState<'quiet' | 'loading' | 'slow'>('quiet')

  useEffect(() => {
    const show = setTimeout(() => setPhase('loading'), SHOW_AFTER_MS)
    const slow = setTimeout(() => setPhase('slow'), SLOW_AFTER_MS)
    return () => {
      clearTimeout(show)
      clearTimeout(slow)
    }
  }, [])

  return (
    <div className="min-h-screen bg-slate-950 grid place-items-center px-4">
      {phase !== 'quiet' && (
        <div role="status" className="flex flex-col items-center gap-3 text-center">
          <LogoMark className="size-10 motion-safe:animate-pulse" />
          <p className="text-slate-400">Loading…</p>
          {phase === 'slow' && (
            <p className="text-sm text-slate-500 max-w-xs">
              Waking up the server. After a quiet spell, the first visit can take up to 15 seconds.
            </p>
          )}
        </div>
      )}
    </div>
  )
}
