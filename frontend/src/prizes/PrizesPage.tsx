import { useState } from 'react'
import { AppShell } from '../components/AppShell'
import { Points } from '../components/PointsIcon'
import { Toast } from '../components/Toast'
import { useToast } from '../components/useToast'
import { ApiError } from '../lib/api'
import { usePointsSummary } from '../points/pointsApi'
import { PrizeDialog } from './PrizeDialog'
import { RedeemDialog } from './RedeemDialog'
import { usePrizes, useRedeemPrize, useSetPrizeArchived, type Prize } from './prizesApi'

type DialogState =
  | { mode: 'closed' }
  | { mode: 'create' }
  | { mode: 'edit'; prize: Prize }
  | { mode: 'redeem'; prize: Prize }

export function PrizesPage() {
  const [showArchived, setShowArchived] = useState(false)
  const [dialog, setDialog] = useState<DialogState>({ mode: 'closed' })
  const prizes = usePrizes(showArchived)
  const summary = usePointsSummary()
  const redeem = useRedeemPrize()
  const setArchived = useSetPrizeArchived()
  const { toast, show } = useToast()
  const balance = summary.data?.balance ?? 0

  async function confirmRedeem(prize: Prize) {
    try {
      await redeem.mutateAsync(prize.id)
      setDialog({ mode: 'closed' })
      show({ tone: 'celebrate', text: `🎉 Enjoy your ${prize.name}! You earned it.` })
    } catch (e) {
      setDialog({ mode: 'closed' })
      show({ tone: 'error', text: e instanceof ApiError ? e.message : 'Redeem failed' })
    }
  }

  return (
    <AppShell>
      <div className="flex items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold">Prize shop</h1>
          <p className="text-slate-400 text-sm">You have <Points value={balance} className="text-amber-300 font-semibold" /> to spend</p>
        </div>
        <button onClick={() => setDialog({ mode: 'create' })} className="rounded-lg bg-amber-400 hover:bg-amber-300 text-slate-950 font-semibold px-4 py-2">
          + Add prize
        </button>
      </div>

      <div className="flex gap-1 mb-4 text-sm" role="tablist">
        {[false, true].map((archived) => (
          <button
            key={String(archived)}
            role="tab"
            aria-selected={showArchived === archived}
            onClick={() => setShowArchived(archived)}
            className={`px-3 py-1.5 rounded-lg ${showArchived === archived ? 'bg-slate-800 text-slate-100' : 'text-slate-400 hover:text-slate-200'}`}
          >
            {archived ? 'Archived' : 'Available'}
          </button>
        ))}
      </div>

      {prizes.isPending && <p className="text-slate-400">Loading prizes…</p>}
      {prizes.isError && <p role="alert" className="text-red-300">Couldn't load prizes: {prizes.error.message}</p>}

      {prizes.data?.length === 0 && (
        <div className="text-center border border-dashed border-slate-800 rounded-2xl py-14 px-6">
          {showArchived ? (
            <p className="text-slate-400">No archived prizes.</p>
          ) : (
            <>
              <p className="text-4xl mb-3">🎁</p>
              <p className="font-medium">No prizes yet</p>
              <p className="text-slate-400 text-sm mt-1">Add a reward worth working for, like a hotpot dinner for 40 points.</p>
            </>
          )}
        </div>
      )}

      <ul className="grid sm:grid-cols-2 gap-3">
        {prizes.data?.map((prize) => (
          <PrizeCard
            key={prize.id}
            prize={prize}
            balance={balance}
            onRedeem={() => setDialog({ mode: 'redeem', prize })}
            onEdit={() => setDialog({ mode: 'edit', prize })}
            onToggleArchive={() => setArchived.mutate({ id: prize.id, archived: !prize.archived })}
          />
        ))}
      </ul>

      {(dialog.mode === 'create' || dialog.mode === 'edit') && (
        <PrizeDialog prize={dialog.mode === 'edit' ? dialog.prize : undefined} onClose={() => setDialog({ mode: 'closed' })} />
      )}
      {dialog.mode === 'redeem' && (
        <RedeemDialog
          prize={dialog.prize}
          balance={balance}
          redeeming={redeem.isPending}
          onConfirm={() => confirmRedeem(dialog.prize)}
          onClose={() => setDialog({ mode: 'closed' })}
        />
      )}
      <Toast toast={toast} />
    </AppShell>
  )
}

type CardProps = {
  prize: Prize
  balance: number
  onRedeem: () => void
  onEdit: () => void
  onToggleArchive: () => void
}

function PrizeCard({ prize, balance, onRedeem, onEdit, onToggleArchive }: CardProps) {
  const affordable = balance >= prize.cost
  const progress = Math.min(100, Math.round((balance / prize.cost) * 100))

  return (
    <li className="bg-slate-900 border border-slate-800 rounded-2xl p-4 flex flex-col">
      <div className="flex items-start gap-3">
        <span className="size-12 shrink-0 grid place-items-center rounded-xl bg-slate-800 text-2xl" aria-hidden>{prize.icon ?? '🎁'}</span>
        <div className="min-w-0 flex-1">
          <p className="font-medium truncate">{prize.name}</p>
          <p className="text-amber-300 font-semibold"><Points value={prize.cost} /></p>
        </div>
        {prize.redeemCount > 0 && (
          <span className="shrink-0 rounded-full bg-amber-400/15 text-amber-300 text-xs font-semibold px-2 py-1" title="Times redeemed">
            ×{prize.redeemCount}
          </span>
        )}
      </div>

      {!prize.archived && (
        <>
          <div className="h-1.5 rounded-full bg-slate-800 overflow-hidden mt-4" role="progressbar" aria-valuenow={progress} aria-valuemin={0} aria-valuemax={100} aria-label={`Progress toward ${prize.name}`}>
            <div className={`h-full transition-all ${affordable ? 'bg-amber-400' : 'bg-slate-500'}`} style={{ width: `${progress}%` }} />
          </div>
          <button
            onClick={onRedeem}
            disabled={!affordable}
            className="mt-3 w-full rounded-lg py-2 font-semibold bg-amber-400 hover:bg-amber-300 text-slate-950 disabled:bg-slate-800 disabled:text-slate-400"
          >
            {affordable ? 'Redeem' : `Need ${prize.cost - balance} more`}
          </button>
        </>
      )}

      <div className="flex justify-end gap-1 mt-2 text-sm">
        {!prize.archived && (
          <button onClick={onEdit} className="px-2 py-1 rounded-lg text-slate-400 hover:bg-slate-800 hover:text-slate-200">Edit</button>
        )}
        <button onClick={onToggleArchive} className="px-2 py-1 rounded-lg text-slate-400 hover:bg-slate-800 hover:text-slate-200">
          {prize.archived ? 'Restore' : 'Archive'}
        </button>
      </div>
    </li>
  )
}
