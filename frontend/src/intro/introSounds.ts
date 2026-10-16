// Small sound effects for the login intro, made with the Web Audio API (no sound files).
// The panda's own sounds (footsteps, coin pickup, happy noises) are in the video's audio track instead.
// Browsers only allow sound after the visitor taps something, so these are created from the "Tap for sound" button.

export type IntroSounds = {
  /** Soft whoosh while the logo drops in. */
  whoosh: () => void
  /** Thump when the logo lands (strength 0–1). */
  land: (strength: number) => void
  /** Pop when the coin comes out of the logo. */
  pop: () => void
  /** Metallic ting when the coin hits the floor (strength 0–1). */
  ting: (strength: number) => void
  close: () => void
}

export function createIntroSounds(): IntroSounds {
  const ctx = new AudioContext()
  void ctx.resume()
  const master = ctx.createGain()
  master.gain.value = 0.35 // a login page should be quiet
  master.connect(ctx.destination)

  /** A gain node that rises fast and fades out exponentially. */
  function envelope(peak: number, attack: number, decay: number, start = ctx.currentTime) {
    const g = ctx.createGain()
    g.gain.setValueAtTime(0.0001, start)
    g.gain.exponentialRampToValueAtTime(peak, start + attack)
    g.gain.exponentialRampToValueAtTime(0.0001, start + attack + decay)
    g.connect(master)
    return g
  }

  function noise(seconds: number) {
    const buffer = ctx.createBuffer(1, Math.ceil(ctx.sampleRate * seconds), ctx.sampleRate)
    const data = buffer.getChannelData(0)
    for (let i = 0; i < data.length; i++) data[i] = Math.random() * 2 - 1
    const source = ctx.createBufferSource()
    source.buffer = buffer
    return source
  }

  function tone(type: OscillatorType, from: number, to: number, glide: number, out: AudioNode, start = ctx.currentTime, length = 0.5) {
    const osc = ctx.createOscillator()
    osc.type = type
    osc.frequency.setValueAtTime(from, start)
    osc.frequency.exponentialRampToValueAtTime(to, start + glide)
    osc.connect(out)
    osc.start(start)
    osc.stop(start + length)
  }

  return {
    whoosh() {
      const t = ctx.currentTime
      const src = noise(0.4)
      const band = ctx.createBiquadFilter()
      band.type = 'bandpass'
      band.Q.value = 1.2
      band.frequency.setValueAtTime(350, t)
      band.frequency.exponentialRampToValueAtTime(1600, t + 0.3)
      src.connect(band).connect(envelope(0.25, 0.12, 0.25, t))
      src.start(t)
    },
    land(strength) {
      const t = ctx.currentTime
      tone('sine', 190, 70, 0.16, envelope(0.9 * strength, 0.004, 0.22, t), t, 0.3)
      const tick = noise(0.03)
      const high = ctx.createBiquadFilter()
      high.type = 'highpass'
      high.frequency.value = 2200
      tick.connect(high).connect(envelope(0.12 * strength, 0.002, 0.03, t))
      tick.start(t)
    },
    pop() {
      const t = ctx.currentTime
      tone('sine', 320, 1150, 0.06, envelope(0.6, 0.003, 0.1, t), t, 0.15)
      const click = noise(0.02)
      const band = ctx.createBiquadFilter()
      band.type = 'bandpass'
      band.frequency.value = 1800
      click.connect(band).connect(envelope(0.2, 0.001, 0.02, t))
      click.start(t)
    },
    ting(strength) {
      // A coin rings with a few partials that aren't exact multiples; higher ones fade faster.
      const t = ctx.currentTime
      const partials: [number, number, number][] = [[2637, 1, 0.45], [3951, 0.5, 0.3], [5274, 0.3, 0.2], [6890, 0.2, 0.12]]
      for (const [freq, level, decay] of partials) {
        tone('sine', freq, freq, 0.01, envelope(0.35 * strength * level, 0.002, decay * (0.5 + strength / 2), t), t, decay + 0.1)
      }
    },
    close() {
      void ctx.close()
    },
  }
}
