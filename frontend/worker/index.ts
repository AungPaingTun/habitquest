// Cloudflare Worker for HabitQuest.
// Static files (the React app) are served by Cloudflare directly; only /api/* reaches this code,
// which forwards the request to the Spring Boot backend on Heroku. The browser sees one origin, so no CORS.

interface Env {
  API_ORIGIN: string
  // Secret (set with `wrangler secret put ORIGIN_KEY`). Heroku only accepts /api requests that carry it,
  // so nobody can skip Cloudflare's rate limit by calling the Heroku URL directly.
  ORIGIN_KEY?: string
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url)
    // Only forward plain /api/... paths. An encoded letter like /%61pi/auth/login would dodge Cloudflare's
    // rate-limit rule (it matches /api/auth/), yet Spring decodes it and still runs the login.
    if (!url.pathname.startsWith('/api/') || url.pathname.includes('%')) {
      return new Response('Not found', { status: 404 })
    }
    if (!env.ORIGIN_KEY) {
      // Heroku rejects requests without the key, so say why here instead of returning confusing 403s.
      return new Response('Server misconfigured: ORIGIN_KEY secret is not set', { status: 500 })
    }
    const target = new URL(url.pathname + url.search, env.API_ORIGIN)
    const forwarded = new Request(target, request)
    // set() also replaces any X-Origin-Key the client sent itself.
    forwarded.headers.set('X-Origin-Key', env.ORIGIN_KEY)
    return fetch(forwarded)
  },
}
