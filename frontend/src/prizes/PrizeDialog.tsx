import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ApiError } from '../lib/api'
import { useCreatePrize, useUpdatePrize, type Prize } from './prizesApi'

const ICONS = ['🍲', '🍕', '🍔', '🍦', '☕', '🎮', '🎬', '🛍️', '📱', '✈️', '💆', '🎁']

type Props = {
  /** The prize to edit, or undefined to create a new one. */
  prize?: Prize
  onClose: () => void
}

export function PrizeDialog({ prize, onClose }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const createPrize = useCreatePrize()
  const updatePrize = useUpdatePrize()

  const [name, setName] = useState(prize?.name ?? '')
  const [icon, setIcon] = useState(prize?.icon ?? '')
  const [cost, setCost] = useState(String(prize?.cost ?? 40))
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const saving = createPrize.isPending || updatePrize.isPending

  useEffect(() => {
    dialogRef.current?.showModal()
  }, [])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setFieldErrors({})
    try {
      if (prize) {
        await updatePrize.mutateAsync({ id: prize.id, name: name.trim(), icon: icon || null })
      } else {
        await createPrize.mutateAsync({ name: name.trim(), icon: icon || null, cost: Number(cost) })
      }
      onClose()
    } catch (e) {
      if (e instanceof ApiError) {
        setFieldErrors(e.fieldErrors)
        if (Object.keys(e.fieldErrors).length === 0) setError(e.message)
      } else {
        setError('Something went wrong')
      }
    }
  }

  return (
    <dialog
      ref={dialogRef}
      onClose={onClose}
      aria-labelledby="prize-dialog-title"
      className="m-auto w-[calc(100%-2rem)] max-w-md rounded-2xl bg-slate-900 text-slate-100 border border-slate-800 p-0 backdrop:bg-black/60"
    >
      <form onSubmit={handleSubmit} noValidate className="p-6 space-y-5">
        <h2 id="prize-dialog-title" className="text-xl font-semibold">{prize ? 'Edit prize' : 'New prize'}</h2>

        {error && (
          <p role="alert" className="text-sm text-red-300 bg-red-950/50 border border-red-900 rounded-lg px-3 py-2">{error}</p>
        )}

        <label className="block">
          <span className="text-sm text-slate-300">What's the reward?</span>
          <input
            autoFocus
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Hotpot dinner"
            maxLength={100}
            className={inputClass(fieldErrors.name)}
          />
          {fieldErrors.name && <span className="text-xs text-red-300 mt-1 block">{fieldErrors.name}</span>}
        </label>

        <fieldset>
          <legend className="text-sm text-slate-300 mb-2">Icon</legend>
          <div className="flex flex-wrap gap-2">
            {ICONS.map((emoji) => (
              <button
                key={emoji}
                type="button"
                onClick={() => setIcon(icon === emoji ? '' : emoji)}
                aria-pressed={icon === emoji}
                aria-label={`Icon ${emoji}`}
                className={`size-10 rounded-lg text-xl border transition-colors ${
                  icon === emoji ? 'border-amber-400 bg-amber-400/15' : 'border-slate-700 hover:border-slate-500'
                }`}
              >
                {emoji}
              </button>
            ))}
          </div>
        </fieldset>

        <label className="block">
          <span className="text-sm text-slate-300">Cost in points</span>
          <input
            type="number"
            inputMode="numeric"
            min={1}
            max={100000}
            value={cost}
            disabled={!!prize}
            onChange={(e) => setCost(e.target.value)}
            className={`${inputClass(fieldErrors.cost)} disabled:opacity-60`}
          />
          {fieldErrors.cost && <span className="text-xs text-red-300 mt-1 block">{fieldErrors.cost}</span>}
          <span className="text-xs text-slate-400 mt-1 block">
            {prize
              ? '🔒 The cost is locked. To change it, archive this prize and create a new one.'
              : "Choose carefully: the cost can't be changed later."}
          </span>
        </label>

        <div className="flex justify-end gap-3 pt-2">
          <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg text-slate-300 hover:bg-slate-800">Cancel</button>
          <button type="submit" disabled={saving} className="px-4 py-2 rounded-lg bg-amber-400 hover:bg-amber-300 disabled:opacity-60 text-slate-950 font-semibold">
            {saving ? 'Saving…' : prize ? 'Save changes' : 'Add prize'}
          </button>
        </div>
      </form>
    </dialog>
  )
}

function inputClass(error?: string) {
  return `mt-1 w-full rounded-lg bg-slate-950 border px-3 py-2 outline-none focus:ring-2 focus:ring-amber-400 ${
    error ? 'border-red-500' : 'border-slate-700'
  }`
}
