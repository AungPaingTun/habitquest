// Picks light or dark before the page draws, so it never flashes the wrong colors.
// A separate file (not inline in index.html) because the Content-Security-Policy only allows our own script files.
// The toggle button and the rest of the logic live in src/components/theme.ts.
(function () {
  var saved = null
  try {
    saved = localStorage.getItem('hq-theme')
  } catch {
    // Storage blocked: fall back to the device setting.
  }
  var theme = saved === 'light' || saved === 'dark' ? saved
    : window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
  document.documentElement.dataset.theme = theme
  // Phone address bar: same color as the page background (must match THEME_COLOR in theme.ts).
  var meta = document.querySelector('meta[name="theme-color"]')
  if (meta) meta.setAttribute('content', theme === 'light' ? '#f1f5f9' : '#020617')
})()
