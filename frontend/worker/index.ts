// Cloudflare Worker for HabitQuest.
// Static files (the React app) are served by Cloudflare directly. Only two kinds of requests reach this code:
//   /api/*   forwarded to the Spring Boot backend on Heroku. The browser sees one origin, so no CORS.
//   /intro/* the login intro videos, served with byte-range support (see serveVideo).

interface Env {
  API_ORIGIN: string
  // Secret (set with `wrangler secret put ORIGIN_KEY`). Heroku only accepts /api requests that carry it,
  // so nobody can skip Cloudflare's rate limit by calling the Heroku URL directly.
  ORIGIN_KEY?: string
  // Cloudflare's static files for this Worker (the "assets" binding in wrangler.jsonc).
  ASSETS: { fetch: (request: Request) => Promise<Response> }
}

const VIDEO_TYPES: Record<string, string> = { '.webm': 'video/webm', '.mov': 'video/quicktime' }

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url)
    if (url.pathname.startsWith('/intro/')) {
      return serveVideo(request, url, env)
    }
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

/**
 * Serves an intro video from the static files, answering byte-range requests with 206 Partial Content.
 * Safari won't play a video otherwise (it starts by asking for bytes 0-1), and Cloudflare's static file
 * serving ignores Range and always sends the whole file. When the file size is known up front, the requested
 * bytes are streamed straight through, so the first bytes go out without waiting for the whole file.
 */
async function serveVideo(request: Request, url: URL, env: Env): Promise<Response> {
  const type = VIDEO_TYPES[url.pathname.slice(url.pathname.lastIndexOf('.'))]
  if (!type) return new Response('Not found', { status: 404 })
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    return new Response('Method not allowed', { status: 405, headers: { Allow: 'GET, HEAD' } })
  }

  // Ask for the whole file (static files would ignore a Range header anyway).
  const asset = await env.ASSETS.fetch(new Request(url.toString(), { method: 'GET' }))
  if (!asset.ok || !asset.body) return asset
  // Stream straight through when the size is known up front; otherwise read the file once to learn its size.
  let size = Number(asset.headers.get('Content-Length'))
  let whole: ArrayBuffer | null = null
  if (!size || asset.headers.has('Content-Encoding')) {
    whole = await asset.arrayBuffer()
    size = whole.byteLength
  }
  const body = () => whole ?? asset.body!

  const headers = new Headers({
    'Content-Type': type,
    'Accept-Ranges': 'bytes',
    // File names carry a version (panda-v1), so browsers may keep them for a week.
    'Cache-Control': 'public, max-age=604800',
    'X-Content-Type-Options': 'nosniff',
  })
  const etag = asset.headers.get('ETag')
  if (etag) headers.set('ETag', etag)

  const range = parseRange(request.headers.get('Range'), size)
  if (range === 'unsatisfiable') {
    if (!whole) await asset.body.cancel()
    headers.set('Content-Range', `bytes */${size}`)
    return new Response(null, { status: 416, headers })
  }
  if (range === null) {
    // No Range header (or one we don't handle, like several ranges): send the whole file.
    headers.set('Content-Length', String(size))
    if (request.method === 'HEAD' && !whole) await asset.body.cancel()
    return new Response(request.method === 'HEAD' ? null : body(), { status: 200, headers })
  }
  const [start, end] = range
  headers.set('Content-Range', `bytes ${start}-${end}/${size}`)
  headers.set('Content-Length', String(end - start + 1))
  if (request.method === 'HEAD') {
    if (!whole) await asset.body.cancel()
    return new Response(null, { status: 206, headers })
  }
  const part = whole ? whole.slice(start, end + 1) : sliceStream(asset.body, start, end)
  return new Response(part, { status: 206, headers })
}

/** Passes on only bytes start..end (inclusive) of a stream, then stops reading the rest. */
function sliceStream(body: ReadableStream<Uint8Array>, start: number, end: number): ReadableStream<Uint8Array> {
  const reader = body.getReader()
  let position = 0
  return new ReadableStream<Uint8Array>({
    async pull(controller) {
      for (;;) {
        const { done, value } = await reader.read()
        if (done) {
          controller.close()
          return
        }
        const chunkStart = position
        position += value.length
        if (position <= start) continue // still before the range
        const from = Math.max(0, start - chunkStart)
        const to = Math.min(value.length, end + 1 - chunkStart)
        if (from < to) controller.enqueue(value.subarray(from, to))
        if (position > end) {
          controller.close()
          await reader.cancel()
        }
        return
      }
    },
    cancel(reason) {
      return reader.cancel(reason)
    },
  })
}

/** "bytes=0-1", "bytes=100-" or "bytes=-500" → [start, end] (inclusive). null = send the whole file. */
function parseRange(header: string | null, size: number): [number, number] | 'unsatisfiable' | null {
  const match = header ? /^bytes=(\d*)-(\d*)$/.exec(header.trim()) : null
  if (!match || (match[1] === '' && match[2] === '')) return null
  let start: number
  let end: number
  if (match[1] === '') {
    // Suffix range: the last N bytes.
    start = Math.max(0, size - Number(match[2]))
    end = size - 1
  } else {
    start = Number(match[1])
    end = match[2] === '' ? size - 1 : Math.min(Number(match[2]), size - 1)
  }
  if (start >= size || start > end) return 'unsatisfiable'
  return [start, end]
}
