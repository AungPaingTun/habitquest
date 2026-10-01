import { useEffect, useRef } from 'react'
import type { Prize } from './prizesApi'

type Props = {
  prize: Prize
  balance: number
  redeeming: boolean
  onConfirm: () => void
  onClose: () => void
}

/** "Are you sure?" step before spending points, since a redeem can't be undone. */
export function RedeemDialog({ prize, balance, redeeming, onConfirm, onClose }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    dialogRef.current?.showModal()
  }, [])

  return (
    <dialog
      ref={dialogRef}
      onClose={onClose}
      aria-labelledby="redeem-title"
      className="m-auto w-[calc(100%-2rem)] max-w-sm rounded-2xl bg-slate-900 text-slate-100 border border-slate-800 p-6 backdrop:bg-black/60"
    >
      <p className="text-5xl text-center" aria-hidden>{prize.icon ?? '🎁'}</p>
      <h2 id="redeem-title" className="text-xl font-semibold text-center mt-3">Redeem {prize.name}?</h2>
      <p className="text-slate-400 text-center mt-2">
        This spends <span className="text-amber-300 font-semibold">🪙 {prize.cost}</span>. You'll have {balance - prize.cost} left.
        Your level and XP don't change.
      </p>
      <div className="flex gap-3 mt-6">
        <button onClick={onClose} className="flex-1 px-4 py-2 rounded-lg text-slate-300 hover:bg-slate-800">Not yet</button>
        <button
          onClick={onConfirm}
          disabled={redeeming}
          autoFocus
          className="flex-1 px-4 py-2 rounded-lg bg-amber-400 hover:bg-amber-300 disabled:opacity-60 text-slate-950 font-semibold"
        >
          {redeeming ? 'Redeeming…' : 'Redeem'}
        </button>
      </div>
    </dialog>
  )
}
