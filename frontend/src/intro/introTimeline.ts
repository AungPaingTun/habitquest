// Timing and geometry for the panda intro on the login page. See PandaIntro.tsx for how it's used.
//
// The video (public/intro/panda-v1.*) is 24 fps, 864×456, transparent, 307 frames:
//   frames 0–35   added walk-in: the clip's first real stride repeated and moved left, so the panda walks in
//                  from fully off-screen instead of popping in
//   then the clip made with Google Veo: +0.0 s walks in · +2.4 s its coin appears · +3.6 s coin in its mouth
//                  · +4–7.5 s happy hops · +8 s turns · +8.5 s walks out to the left
//   last 1.3 s     added walk-out: its last two real steps repeated and moved left, so it keeps walking at its own
//                  speed until it's gone
// All "video" times below are video seconds; the page plays the video at SPEED.

export const SPEED = 1.5

const PRE = 36 / 24

export const VIDEO = {
  length: 307 / 24,
  width: 864,
  ground: 440, // y of the floor in the video
  coinShow: PRE + 2.4, // the video's own coin is hidden before this, so the page's falling coin can land on it
  turned: PRE + 8.4, // the panda has turned and is walking away
  coin: { x: 639.5, y: 426, width: 113 }, // centre and width of the coin lying on the floor
}

// The panda's right edge in every frame (video pixels). The form only fades in once the panda has walked past
// its left edge, so they never overlap (on phones that means once the panda has left the screen).
export const RIGHT_EDGE = [
  0, 3, 9, 15, 23, 29, 36, 43, 52, 58, 66, 73, 81, 87, 94, 101, 107, 113, 119, 125, 131, 138, 145, 152,
  159, 166, 173, 179, 186, 193, 199, 205, 213, 219, 226, 233, 242, 248, 256, 263, 271, 277, 284, 291, 297, 303, 309, 315,
  321, 328, 335, 342, 349, 356, 363, 369, 376, 383, 389, 395, 403, 409, 416, 423, 431, 438, 445, 452, 459, 465, 472, 478,
  483, 489, 495, 500, 506, 512, 519, 525, 532, 538, 544, 550, 557, 563, 568, 574, 580, 586, 592, 599, 606, 613, 695, 695,
  695, 695, 695, 695, 695, 695, 695, 695, 695, 695, 695, 695, 695, 697, 701, 707, 712, 715, 716, 717, 717, 717, 717, 717,
  719, 723, 726, 728, 728, 726, 721, 714, 706, 696, 699, 701, 701, 701, 702, 702, 702, 702, 702, 702, 702, 702, 702, 701,
  700, 697, 694, 691, 688, 688, 688, 688, 689, 689, 689, 689, 689, 689, 688, 688, 688, 688, 689, 689, 691, 693, 696, 699,
  701, 703, 703, 702, 702, 701, 702, 703, 704, 706, 709, 713, 720, 729, 741, 751, 759, 766, 770, 773, 773, 774, 773, 773,
  772, 770, 770, 771, 775, 781, 789, 797, 804, 811, 816, 822, 824, 825, 825, 825, 824, 824, 824, 824, 825, 825, 826, 827,
  828, 829, 830, 831, 832, 836, 840, 844, 845, 845, 841, 831, 815, 790, 775, 764, 746, 719, 704, 685, 670, 663, 670, 675,
  680, 683, 684, 682, 683, 678, 664, 656, 651, 644, 636, 627, 616, 606, 595, 585, 573, 562, 550, 538, 525, 512, 500, 492,
  491, 489, 475, 450, 435, 425, 414, 403, 390, 379, 366, 360, 343, 333, 321, 310, 298, 286, 273, 260, 248, 240, 239, 237,
  223, 198, 183, 173, 162, 151, 138, 127, 114, 108, 91, 81, 69, 58, 46, 34, 21, 8, 0,
]

// Page moments (page seconds). The coin must be resting before VIDEO.coinShow / SPEED.
export const LOGO_IN = 0.35
export const COIN_DROP = 1.25
export const COIN_LAND = COIN_DROP + 0.38
export const COIN_REST = COIN_DROP + 0.95
export const FORM_FADE = 0.6

type Ease = (p: number) => number
export const ease = {
  linear: ((p) => p) as Ease,
  in: ((p) => p * p) as Ease,
  out: ((p) => 1 - (1 - p) * (1 - p)) as Ease,
  outCubic: ((p) => 1 - Math.pow(1 - p, 3)) as Ease,
  inOut: ((p) => (p < 0.5 ? 2 * p * p : 1 - Math.pow(-2 * p + 2, 2) / 2)) as Ease,
}

/** Keyframes → value at time t. Each key is [time, value, easing to the next key]. */
export function keyframes(t: number, keys: [number, number, Ease?][]): number {
  if (t <= keys[0][0]) return keys[0][1]
  for (let i = 0; i < keys.length - 1; i++) {
    const [t0, v0, e] = keys[i]
    const [t1, v1] = keys[i + 1]
    if (t < t1) return v0 + (v1 - v0) * (e ?? ease.inOut)((t - t0) / (t1 - t0))
  }
  return keys[keys.length - 1][1]
}

/** Smooth 0 → 1 → 0 bump centred on t0 (used for little squashes on impact). */
export function bump(t: number, t0: number, halfWidth: number): number {
  return Math.abs(t - t0) < halfWidth ? 0.5 * (1 + Math.cos((Math.PI * (t - t0)) / halfWidth)) : 0
}

export const clamp01 = (x: number) => Math.min(1, Math.max(0, x))
