import { useEffect, useLayoutEffect, useRef, type RefObject } from 'react'
import { LogoMark } from '../components/Logo'
import coinUrl from './coin.png'
import {
  COIN_DROP, COIN_LAND, COIN_REST, FORM_FADE, LOGO_IN, RIGHT_EDGE, SPEED, VIDEO,
  bump, clamp01, ease, keyframes,
} from './introTimeline'

// Safari can't show WebM with transparency, so it gets an HEVC .mov (with premultiplied colour) instead.
const isSafari = /^((?!chrome|android|crios|fxios).)*safari/i.test(navigator.userAgent)
const VIDEO_URL = isSafari ? '/intro/panda-v1.mov' : '/intro/panda-v1.webm'
// If the video can't start this fast (slow network), skip the intro rather than keep the form hidden.
const LOAD_BUDGET_MS = 2500

type Props = {
  /** The login form; hidden during the intro, then faded in once the panda is out of its way. */
  cardRef: RefObject<HTMLElement | null>
  /** Called when the intro has finished or been skipped. */
  onDone: () => void
}

/**
 * Login intro: the logo drops in and throws a gold coin, a panda (a real video) walks in, picks the coin up
 * with its mouth, hops happily, turns and walks away; then the login form fades in.
 * Everything is driven by one clock that follows the video frame on screen, so the falling coin lands on the
 * exact spot of the video's coin. Click, Esc or "Skip intro" ends it at once.
 */
export function PandaIntro({ cardRef, onDone }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const logoRef = useRef<HTMLDivElement>(null)
  const coinRef = useRef<HTMLImageElement>(null)
  const skipRef = useRef<HTMLButtonElement>(null)
  const onDoneRef = useRef(onDone)
  useLayoutEffect(() => {
    onDoneRef.current = onDone
  })

  // Hide the form before the first paint, so it never flashes.
  useLayoutEffect(() => {
    const card = cardRef.current
    if (!card) return
    card.style.opacity = '0'
    card.inert = true
    return () => {
      card.style.opacity = ''
      card.style.transform = ''
      card.inert = false
    }
  }, [cardRef])

  useEffect(() => {
    const video = videoRef.current!, logo = logoRef.current!, coin = coinRef.current!, card = cardRef.current
    let raf = 0, finished = false, formShowing = false

    const finish = () => {
      if (finished) return
      finished = true
      cancelAnimationFrame(raf)
      video.pause()
      onDoneRef.current()
    }

    // ---- Layout (page pixels). The video's floor sits on the page's "ground" line at 82% height.
    let W = 0, H = 0, G = 0, k = 1, formStart = 99
    const layout = () => {
      W = window.innerWidth
      H = window.innerHeight
      G = H * 0.82
      k = Math.max(0.45, Math.min(0.68, W / 1750)) // video scale: smaller on phones
      video.style.width = `${VIDEO.width * k}px`
      video.style.transform = `translate(0px, ${G - VIDEO.ground * k}px)`
      coin.style.width = `${VIDEO.coin.width * k}px`
      // The form fades in from the first frame (after the turn) where the panda is left of it.
      const cardLeft = card ? card.getBoundingClientRect().left : 0
      formStart = VIDEO.length / SPEED + 0.05
      for (let i = Math.round(VIDEO.turned * 24); i < RIGHT_EDGE.length; i++) {
        if (RIGHT_EDGE[i] * k < cardLeft - 10) { formStart = i / 24 / SPEED; break }
      }
    }

    // ---- Clock. requestVideoFrameCallback tells us exactly which frame is on screen and when (video.currentTime
    // updates in small jumps in some browsers); between frames we move on with the wall clock.
    let shownMedia = 0, shownAt = 0, endedAt: number | null = null
    const hasFrameCallback = 'requestVideoFrameCallback' in video
    const onFrame: VideoFrameRequestCallback = (now, meta) => {
      shownMedia = meta.mediaTime
      shownAt = now
      video.requestVideoFrameCallback(onFrame)
    }
    if (hasFrameCallback) video.requestVideoFrameCallback(onFrame)
    const shownFrameTime = () => (hasFrameCallback && shownAt ? shownMedia : video.currentTime)
    const pageTime = (now: number) => {
      const vt = hasFrameCallback && shownAt && !video.paused
        ? Math.min(shownMedia + ((now - shownAt) / 1000) * video.playbackRate, VIDEO.length)
        : video.currentTime
      if (vt < VIDEO.length - 0.01 && !video.ended) { endedAt = null; return vt / SPEED }
      endedAt ??= now
      return VIDEO.length / SPEED + (now - endedAt) / 1000 // keeps running after the clip, for the form fade
    }

    // ---- One frame of the scene.
    const draw = (now: number) => {
      const t = pageTime(now)

      // Logo: drops in and settles, a little hop "throws" the coin, later floats up and fades.
      const away = formStart - 0.5
      const y = keyframes(t, [
        [0, -H * 0.5, ease.in], [LOGO_IN, 0, ease.out], [LOGO_IN + 0.15, -34, ease.in], [LOGO_IN + 0.3, 0, ease.out],
        [LOGO_IN + 0.38, -8, ease.in], [LOGO_IN + 0.46, 0],
        [COIN_DROP - 0.28, 0, ease.out], [COIN_DROP - 0.14, -24, ease.in], [COIN_DROP, 0],
        [away, 0, ease.inOut], [away + 0.7, -H * 0.12],
      ])
      const squash = 0.12 * bump(t, LOGO_IN, 0.08) + 0.05 * bump(t, LOGO_IN + 0.3, 0.06) + 0.1 * bump(t, COIN_DROP, 0.08)
      const leave = keyframes(t, [[away, 0, ease.inOut], [away + 0.7, 1]])
      const size = 1 - 0.6 * leave
      logo.style.transform = `translate(${W / 2 - 48}px, ${H * 0.35 - 48 + y}px) translate(48px, 96px) `
        + `scale(${(1 + squash) * size}, ${(1 - squash) * size}) translate(-48px, -96px)`
      logo.style.opacity = String(1 - leave)

      // Coin: falls from the logo with a gentle wobble, bounces twice with a small squash, rests exactly on the
      // video's coin, and hides on the very frame the video's coin appears (never two coins at once).
      const cw = VIDEO.coin.width * k, ch = coin.offsetHeight || cw * 0.28
      const restX = VIDEO.coin.x * k, restY = G - VIDEO.ground * k + VIDEO.coin.y * k
      const startX = W / 2, startY = H * 0.35
      const cx = keyframes(t, [[COIN_DROP, startX, ease.linear], [COIN_LAND, restX + (startX - restX) * 0.3, ease.linear],
        [COIN_LAND + 0.36, restX + (startX - restX) * 0.06, ease.linear], [COIN_REST, restX]])
      const cy = keyframes(t, [[COIN_DROP, startY, ease.in], [COIN_LAND, restY, ease.out], [COIN_LAND + 0.2, restY - 34, ease.in],
        [COIN_LAND + 0.38, restY, ease.out], [COIN_LAND + 0.48, restY - 10, ease.in], [COIN_REST, restY]])
      const fall = clamp01((t - COIN_DROP) / (COIN_LAND - COIN_DROP))
      const settle = clamp01((t - COIN_LAND) / (COIN_REST - COIN_LAND))
      const wobble = t < COIN_LAND
        ? Math.sin((t - COIN_DROP) * 14) * 24 * (1 - fall * 0.5)
        : Math.sin((t - COIN_LAND) * 22) * 10 * (1 - settle)
      const hit = 0.16 * bump(t, COIN_LAND, 0.06) + 0.09 * bump(t, COIN_LAND + 0.38, 0.05) + 0.04 * bump(t, COIN_REST, 0.05)
      const grow = keyframes(t, [[COIN_DROP, 0.6, ease.out], [COIN_DROP + 0.25, 1]])
      coin.style.opacity = t >= COIN_DROP && shownFrameTime() < VIDEO.coinShow ? '1' : '0'
      coin.style.transform = `translate(${cx - cw / 2}px, ${cy - ch / 2}px) rotate(${wobble}deg) `
        + `scale(${grow * (1 + hit)}, ${grow * (1 - 1.6 * hit)})`

      // Form: fades and rises in once the panda is out of its way.
      if (card) {
        const shown = keyframes(t, [[formStart, 0, ease.outCubic], [formStart + FORM_FADE, 1]])
        card.style.opacity = String(shown)
        card.style.transform = `translateY(${(1 - shown) * 20}px)`
        if (shown > 0 && !formShowing) {
          // The form is usable from here on; nothing left to skip.
          formShowing = true
          card.inert = false
          if (skipRef.current) skipRef.current.style.visibility = 'hidden'
        }
      }

      // End only once the form is in AND the panda has walked off the screen (on wide screens the form appears
      // while the panda is still walking out beside it; ending earlier would make the panda vanish mid-step).
      if (t >= Math.max(formStart + FORM_FADE, VIDEO.length / SPEED)) finish()
      else raf = requestAnimationFrame(draw)
    }

    // ---- Start as soon as the video can play (it keeps loading while it plays); skip the intro if it's slow to
    // start or the browser blocks autoplay (for example iPhone Low Power Mode).
    layout()
    window.addEventListener('resize', layout)
    const budget = window.setTimeout(finish, LOAD_BUDGET_MS)
    const start = () => {
      window.clearTimeout(budget)
      if (finished) return
      video.playbackRate = SPEED
      video.play().then(() => { raf = requestAnimationFrame(draw) }, finish)
    }
    if (video.readyState >= HTMLMediaElement.HAVE_FUTURE_DATA) start()
    else video.addEventListener('canplay', start, { once: true })
    video.addEventListener('error', finish, { once: true })

    // Click anywhere or press Esc to skip, until the form shows (then clicks are for the form, and the panda
    // just finishes walking off).
    const skip = () => { if (!formShowing) finish() }
    const skipOnKey = (e: KeyboardEvent) => { if (e.key === 'Escape') skip() }
    window.addEventListener('pointerdown', skip)
    window.addEventListener('keydown', skipOnKey)

    return () => {
      finished = true
      cancelAnimationFrame(raf)
      window.clearTimeout(budget)
      video.pause()
      window.removeEventListener('resize', layout)
      window.removeEventListener('pointerdown', skip)
      window.removeEventListener('keydown', skipOnKey)
      video.removeEventListener('canplay', start)
    }
  }, [cardRef])

  return (
    <>
      {/* The scene is decoration only, so screen readers skip it. */}
      <div className="fixed inset-0 z-10 overflow-hidden pointer-events-none" aria-hidden>
        <video
          ref={videoRef}
          src={VIDEO_URL}
          muted
          playsInline
          preload="auto"
          disablePictureInPicture
          className="absolute left-0 top-0 max-w-none origin-top-left"
        />
        <img ref={coinRef} src={coinUrl} alt="" className="absolute left-0 top-0 max-w-none origin-bottom opacity-0" />
        <div ref={logoRef} className="absolute left-0 top-0 size-24 opacity-0">
          <LogoMark className="size-24" />
        </div>
      </div>
      <button
        ref={skipRef}
        type="button"
        onClick={() => onDoneRef.current()}
        className="fixed bottom-4 right-4 z-20 rounded-lg px-3 py-1.5 text-sm text-slate-400 hover:text-slate-100 focus-visible:text-slate-100 focus-visible:outline-2 focus-visible:outline-emerald-500"
      >
        Skip intro
      </button>
    </>
  )
}
