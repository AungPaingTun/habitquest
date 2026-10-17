import { useSyncExternalStore } from 'react'

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

/** Switches the theme and remembers the choice in this browser. */
export function setTheme(theme: Theme) {
  try {
    localStorage.setItem(STORAGE_KEY, theme)
  } catch {
    // Private mode or blocked storage: the switch still works for this visit.
  }
  apply(theme)
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
