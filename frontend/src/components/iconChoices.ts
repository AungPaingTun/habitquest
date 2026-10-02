export type IconGroup = { name: string; icons: string[] }

export const HABIT_ICONS: IconGroup[] = [
  { name: 'Health', icons: ['🥗', '🍎', '🥦', '💧', '🍵', '😴', '🦷', '💊', '🚭', '🚫', '🧴', '🩺'] },
  { name: 'Fitness', icons: ['🏃', '💪', '🚴', '🏊', '🧗', '⚽', '🏀', '🏸', '🥊', '🚶', '🤸', '🏋️'] },
  { name: 'Mind', icons: ['🧘', '🙏', '📓', '🌅', '🌙', '😊', '🫁', '📵', '🌿', '☮️'] },
  { name: 'Learning', icons: ['📚', '✍️', '🎓', '🧠', '💻', '🗣️', '🎹', '🎸', '🎨', '🔬', '🌍', '📝'] },
  { name: 'Home & life', icons: ['🧹', '🧺', '🍳', '🪴', '🛏️', '🐶', '💰', '📅', '📞', '❤️', '🎯', '⭐'] },
]

export const PRIZE_ICONS: IconGroup[] = [
  { name: 'Food & drink', icons: ['🍲', '🍕', '🍔', '🍣', '🍜', '🍗', '🍰', '🍦', '🧋', '☕', '🍩', '🍫'] },
  { name: 'Fun', icons: ['🎮', '🎬', '🎤', '🎳', '🎢', '🎟️', '📺', '🎧', '🎲', '🏖️'] },
  { name: 'Shopping', icons: ['🛍️', '👟', '👕', '👜', '📱', '⌚', '💍', '📖', '💄', '🎁'] },
  { name: 'Self-care & trips', icons: ['💆', '🛁', '💅', '😴', '✈️', '🚗', '🏕️', '🌴', '🏨', '🎉'] },
]

/** The first character the user typed, keeping multi-part emoji (skin tones, flags, 👨‍👩‍👧) intact. */
export function firstGrapheme(text: string): string {
  const trimmed = text.trim()
  if (!trimmed) return ''
  const segmenter = new Intl.Segmenter(undefined, { granularity: 'grapheme' })
  const first = segmenter.segment(trimmed)[Symbol.iterator]().next().value
  return first?.segment ?? ''
}
