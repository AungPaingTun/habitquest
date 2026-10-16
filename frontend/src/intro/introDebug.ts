// Debug log for the login intro, for checking on phones where there's no console:
// open the login page with ?introdebug=1 and the login page shows what the intro did and why it ended.
// Without that query parameter nothing is recorded or shown.

export const INTRO_DEBUG = new URLSearchParams(window.location.search).has('introdebug')

const lines: string[] = []
const pageStart = performance.now()

export function introLog(message: string) {
  if (!INTRO_DEBUG) return
  lines.push(`${((performance.now() - pageStart) / 1000).toFixed(2)}s ${message}`)
}

export function introLogText() {
  return lines.join('\n')
}
