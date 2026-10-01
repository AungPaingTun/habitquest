import { useCallback, useEffect, useRef, useState } from 'react'

export type ToastMessage = { text: string; tone: 'success' | 'error' | 'celebrate' }

/** A tiny one-at-a-time notification that disappears after a few seconds. Render it with <Toast>. */
export function useToast() {
  const [toast, setToast] = useState<ToastMessage | null>(null)
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined)

  const show = useCallback((message: ToastMessage) => {
    clearTimeout(timer.current)
    setToast(message)
    timer.current = setTimeout(() => setToast(null), message.tone === 'celebrate' ? 5000 : 3000)
  }, [])

  useEffect(() => () => clearTimeout(timer.current), [])

  return { toast, show }
}
