import type { ToastMessage } from './useToast'

const TONES = {
  success: 'bg-emerald-500 text-slate-950',
  celebrate: 'bg-amber-400 text-slate-950',
  error: 'bg-red-500 text-white',
}

export function Toast({ toast }: { toast: ToastMessage | null }) {
  return (
    <div aria-live="polite" className="fixed bottom-6 inset-x-0 flex justify-center px-4 pointer-events-none z-50">
      {toast && (
        <div className={`rounded-xl px-4 py-3 font-semibold shadow-lg ${TONES[toast.tone]}`}>{toast.text}</div>
      )}
    </div>
  )
}
