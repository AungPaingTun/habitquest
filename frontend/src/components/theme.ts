import { useSyncExternalStore } from 'react'
import { flushSync } from 'react-dom'

// Browsers that support View Transitions expose this; older ones don't, so it stays optional.
type ViewTransitionDocument = Document & {
  startViewTransition?: (callback: () => void) => { ready: Promise<void> }
}

// Light or dark colors. public/theme-init.js picks the starting theme before the page draws (saved choice, else the
// device setting, else dark) and stamps it on <html data-theme="...">; index.css swaps the colors from there.

export type Theme = 'light' | 'dark'

const STORAGE_KEY = 'hq-theme'
// Page background per theme (slate-950 in dark, its light stand-in), for the phone's address bar.
const THEME_COLOR: Record<Theme, string> = { dark: '#020617', light: '#f1f5f9' }
const listeners = new Set<() => void>()

export function currentTheme(): Theme {
  return document.documentElement.dataset.theme === 'light' ? 'light' : 'dark'
}

function apply(theme: Theme) {
  document.documentElement.dataset.theme = theme
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', THEME_COLOR[theme])
  listeners.forEach((listener) => listener())
}

/**
 * Switches the theme and remembers the choice in this browser.
 * With `origin` (the toggle button's center), the new colors are revealed in a circle growing from that point.
 * Browsers without View Transitions, or visitors who prefer reduced motion, get an instant switch.
 */
export function setTheme(theme: Theme, origin?: { x: number; y: number }) {
  try {
    localStorage.setItem(STORAGE_KEY, theme)
  } catch {
    // Private mode or blocked storage: the switch still works for this visit.
  }

  const doc = document as ViewTransitionDocument
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (!doc.startViewTransition || reduceMotion || !origin) {
    apply(theme)
    return
  }

  // flushSync so the icon swap and chart colors are part of the snapshot the browser animates.
  const transition = doc.startViewTransition(() => flushSync(() => apply(theme)))
  transition.ready
    .then(() => {
      const { x, y } = origin
      const endRadius = Math.hypot(Math.max(x, innerWidth - x), Math.max(y, innerHeight - y))
      document.documentElement.animate(
        { clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${endRadius}px at ${x}px ${y}px)`] },
        { duration: 350, easing: 'ease-in-out', pseudoElement: '::view-transition-new(root)' },
      )
    })
    .catch(() => {
      // The colors have already switched; only the reveal animation was skipped.
    })
}

// Until the visitor picks a theme here, follow the device when it switches (e.g. automatic dark mode at night).
window.matchMedia('(prefers-color-scheme: light)').addEventListener('change', (e) => {
  let saved: string | null = null
  try {
    saved = localStorage.getItem(STORAGE_KEY)
  } catch {
    // No storage: treat as "not chosen".
  }
  if (!saved) apply(e.matches ? 'light' : 'dark')
})

// A switch in another open tab of the app applies here too.
window.addEventListener('storage', (e) => {
  if (e.key === STORAGE_KEY && (e.newValue === 'light' || e.newValue === 'dark')) apply(e.newValue)
})

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

/** The current theme; the component re-renders when it changes. */
export function useTheme(): Theme {
  return useSyncExternalStore(subscribe, currentTheme)
}
