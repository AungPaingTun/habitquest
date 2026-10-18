import { useEffect, useLayoutEffect, useRef, useState, type RefObject } from 'react'
import { LogoMark } from '../components/Logo'
import coinUrl from './coin.png'
import {
  COIN_DROP, COIN_LAND, COIN_REST, LOGO_IN, SPEED, VIDEO,
  bump, clamp01, ease, keyframes,
} from './introTimeline'
import { INTRO_DEBUG, introLog as log } from './introDebug'
import { createIntroSounds, type IntroSounds } from './introSounds'

// Safari can't show WebM with transparency, so it gets an HEVC .mov (with premultiplied colour) instead.
const isSafari = /^((?!chrome|android|crios|fxios).)*safari/i.test(navigator.userAgent)
const VIDEO_URL = isSafari ? '/intro/panda-v2.mov' : '/intro/panda-v2.webm'
// If the video hasn't started playing this fast (slow network), skip the intro rather than keep the form hidden.
const LOAD_BUDGET_MS = 3000

// Logo and coin sounds, in page seconds (they match the motion in draw()).
const SOUND_EVENTS: [number, (s: IntroSounds) => void][] = [
  [0.02, (s) => s.whoosh()], // logo drops in
  [LOGO_IN, (s) => s.land(1)],
  [LOGO_IN + 0.3, (s) => s.land(0.35)], // small second bounce
  [COIN_DROP, (s) => { s.land(0.5); s.pop() }], // the hop that throws the coin out
  [COIN_LAND, (s) => s.ting(1)],
  [COIN_LAND + 0.38, (s) => s.ting(0.55)],
  [COIN_REST, (s) => s.ting(0.3)],
]

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
 * It tries to start with sound (the panda's own audio track plus small logo and coin sounds). Browsers block sound
 * until the visitor has interacted with the site, so usually it starts muted with a "Tap for sound" button.
 */
export function PandaIntro({ cardRef, onDone }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const logoRef = useRef<HTMLDivElement>(null)
  const coinRef = useRef<HTMLImageElement>(null)
  const skipRef = useRef<HTMLButtonElement>(null)
  const soundsRef = useRef<IntroSounds | null>(null)
  const [soundOn, setSoundOn] = useState(false)
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
    // The form slides in from off the right edge; clip the page so that never adds a horizontal scrollbar.
    const root = document.documentElement
    const prevOverflowX = root.style.overflowX
    root.style.overflowX = 'hidden'
    return () => {
      card.style.opacity = ''
      card.style.transform = ''
      card.inert = false
      root.style.overflowX = prevOverflowX
    }
  }, [cardRef])

  // Must run inside the tap itself: that's what lets the browser play sound.
  function toggleSound() {
    const video = videoRef.current
    if (!video) return
    if (!soundOn) {
      soundsRef.current ??= createIntroSounds()
      video.muted = false
    } else {
      video.muted = true
    }
    setSoundOn(!soundOn)
  }
  useEffect(() => () => soundsRef.current?.close(), [])

  useEffect(() => {
    const video = videoRef.current!, logo = logoRef.current!, coin = coinRef.current!, card = cardRef.current
    let raf = 0, finished = false, formShowing = false, lastT = 0, maxT = 0

    log(`${isSafari ? 'Safari' : 'other browser'} → ${VIDEO_URL}`)

    const finish = (reason = 'done') => {
      if (finished) return
      log(`intro ended: ${reason}`)
      finished = true
      cancelAnimationFrame(raf)
      video.pause()
      onDoneRef.current()
    }

    // ---- Layout (page pixels). The video's floor sits on the page's "ground" line at 82% height.
    let W = 0, H = 0, G = 0, k = 1, formStart = 99, slideFrom = 0
    const layout = () => {
      W = window.innerWidth
      H = window.innerHeight
      G = H * 0.82
      // Video scale: smaller on phones. The video sits at the left edge (translate x 0), so the panda walks in
      // from the screen edge, picks the coin up part-way across, then walks back out to the edge.
      k = Math.max(0.45, Math.min(0.72, W / 1750))
      video.style.width = `${VIDEO.width * k}px`
      video.style.transform = `translate(0px, ${G - VIDEO.ground * k}px)`
      coin.style.width = `${VIDEO.coin.width * k}px`
      // How far off the right edge the login form starts before it slides in to its centred spot.
      slideFrom = W / 2 + (card ? card.offsetWidth : 360) / 2 + 24
      // The form starts sliding in just after the panda turns and finishes as it walks off screen, so the panda
      // appears to pull the form in behind it.
      formStart = VIDEO.turned / SPEED + 0.1
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
      // The clock only ever moves forward. Safari's .mov can report its time jumping back (a re-seek on the
      // byte-range stream), which would otherwise rewind the scene and replay the logo drop.
      maxT = Math.max(maxT, pageTime(now))
      const t = maxT

      // Sounds fire as the clock passes their moment (only once the visitor has turned sound on).
      const sounds = soundsRef.current
      if (sounds && !video.muted) {
        for (const [at, play] of SOUND_EVENTS) if (lastT < at && t >= at) play(sounds)
      }
      lastT = t

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

      // Form: slides in from off the right edge and settles at centre, following the panda as it walks out left.
      // It travels over the whole walk-out (turn → off screen), so the panda seems to pull it in.
      if (card) {
        const slideEnd = VIDEO.length / SPEED
        const shown = keyframes(t, [[formStart, 0, ease.inOut], [slideEnd, 1]])
        // Opacity fades in over the first half of the slide, so the card is faintly visible as it travels.
        card.style.opacity = String(clamp01((t - formStart) / ((slideEnd - formStart) * 0.5)))
        card.style.transform = `translateX(${(1 - shown) * slideFrom}px)`
        if (shown > 0 && !formShowing) {
          // The form is usable from here on; nothing left to skip.
          formShowing = true
          card.inert = false
          if (skipRef.current) skipRef.current.style.visibility = 'hidden'
        }
      }

      // End only once the form is in AND the panda has walked off the screen (on wide screens the form appears
      // while the panda is still walking out beside it; ending earlier would make the panda vanish mid-step).
      if (t >= VIDEO.length / SPEED) finish('finished normally')
      else raf = requestAnimationFrame(draw)
    }

    // ---- Press play right away: iPhones don't download a video until it's asked to play (waiting for "ready"
    // first never finishes there). The scene starts drawing once the video is really playing. Skip the intro if
    // that takes too long or the browser refuses autoplay (for example iPhone Low Power Mode).
    layout()
    window.addEventListener('resize', layout)
    const budget = window.setTimeout(() => finish(`video didn't start within ${LOAD_BUDGET_MS / 1000}s`), LOAD_BUDGET_MS)
    const begin = () => {
      window.clearTimeout(budget)
      if (finished) return
      video.playbackRate = SPEED // iPhones can reset the speed while loading
      raf = requestAnimationFrame(draw)
    }
    video.defaultPlaybackRate = SPEED
    video.playbackRate = SPEED
    // Try with sound first. Browsers allow it only if the visitor has already interacted with the site (or
    // allowed sound for it); otherwise they refuse, and the intro plays muted with "Tap for sound".
    video.muted = false
    video.play().then(
      () => {
        if (finished) return
        log('playing with sound')
        soundsRef.current ??= createIntroSounds()
        setSoundOn(true)
        begin()
      },
      (soundError: Error) => {
        log(`sound refused (${soundError.name}), playing muted`)
        video.muted = true
        video.play().then(
          () => { log('playing muted'); begin() },
          (error: Error) => finish(`autoplay refused (${error.name}); Low Power Mode?`),
        )
      },
    )
    const onError = () => finish(`video error ${video.error?.code ?? ''} ${video.error?.message ?? ''}`)
    video.addEventListener('error', onError, { once: true })
    if (INTRO_DEBUG) {
      for (const name of ['loadedmetadata', 'canplay', 'playing', 'waiting', 'stalled', 'ended']) {
        video.addEventListener(name, () => log(`${name} (video at ${video.currentTime.toFixed(2)}s, rate ${video.playbackRate})`))
      }
    }

    // Click anywhere or press Esc to skip, until the form shows (then clicks are for the form, and the panda
    // just finishes walking off).
    const skip = (e: Event) => {
      // Taps on the intro's own buttons (sound, skip) aren't "click anywhere to skip".
      if (e.target instanceof Element && e.target.closest('[data-intro-controls]')) return
      if (!formShowing) finish('skipped by a tap')
    }
    const skipOnKey = (e: KeyboardEvent) => { if (e.key === 'Escape' && !formShowing) finish('skipped with Esc') }
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
      video.removeEventListener('error', onError)
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
      <div data-intro-controls className="fixed bottom-4 right-4 z-20 flex items-center gap-1">
        <button
          type="button"
          onClick={toggleSound}
          aria-pressed={soundOn}
          className={buttonClass}
        >
          <SpeakerIcon on={soundOn} />
          {soundOn ? 'Sound on' : 'Tap for sound'}
        </button>
        <button ref={skipRef} type="button" onClick={() => onDoneRef.current()} className={buttonClass}>
          Skip intro
        </button>
      </div>

    </>
  )
}

const buttonClass = 'flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm text-slate-400 hover:text-slate-100 '
  + 'focus-visible:text-slate-100 focus-visible:outline-2 focus-visible:outline-emerald-500'

function SpeakerIcon({ on }: { on: boolean }) {
  return (
    <svg viewBox="0 0 24 24" aria-hidden className="size-4 shrink-0" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 9.5v5h3.5L12 18.5v-13L7.5 9.5H4z" fill="currentColor" />
      {on
        ? <path d="M15.5 9a4 4 0 0 1 0 6M18 6.5a7.5 7.5 0 0 1 0 11" />
        : <path d="M16 9.5l5 5M21 9.5l-5 5" />}
    </svg>
  )
}
