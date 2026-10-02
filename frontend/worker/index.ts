// Cloudflare Worker for HabitQuest.
// Static files (the React app) are served by Cloudflare directly; only /api/* reaches this code,
// which forwards the request to the Spring Boot backend on Heroku. The browser sees one origin, so no CORS.

interface Env {
  API_ORIGIN: string
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url)
    const target = new URL(url.pathname + url.search, env.API_ORIGIN)
    return fetch(new Request(target, request))
  },
}
