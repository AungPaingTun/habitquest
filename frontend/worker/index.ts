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
    const target = new URL(url.pathname + url.search, env.API_ORIGIN)
    const forwarded = new Request(target, request)
    if (env.ORIGIN_KEY) {
      forwarded.headers.set('X-Origin-Key', env.ORIGIN_KEY)
    }
    return fetch(forwarded)
  },
}
