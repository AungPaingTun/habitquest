import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../lib/api'
import { todayIn, useCreateHabit, useUpdateHabit, type Frequency, type Habit, type HabitInput } from './habitsApi'

const ICONS = ['🥗', '🏃', '💪', '📚', '💧', '🧘', '😴', '🦷', '✍️', '🎯', '🚭', '🧹']

const FREQUENCIES: { value: Frequency; label: string; max: number }[] = [
  { value: 'DAILY', label: 'Daily', max: 1 },
  { value: 'WEEKLY', label: 'Weekly', max: 7 },
  { value: 'MONTHLY', label: 'Monthly', max: 31 },
]

type Props = {
  /** The habit to edit, or undefined to create a new one. */
  habit?: Habit
  onClose: () => void
}

/** Add/edit form shown in a native <dialog>, which gives us Esc-to-close, a backdrop and focus trapping for free. */
export function HabitDialog({ habit, onClose }: Props) {
  const { user } = useAuth()
  const dialogRef = useRef<HTMLDialogElement>(null)
  const createHabit = useCreateHabit()
  const updateHabit = useUpdateHabit()

  const [name, setName] = useState(habit?.name ?? '')
  const [icon, setIcon] = useState(habit?.icon ?? '')
  const [points, setPoints] = useState(String(habit?.points ?? 2))
  const [frequency, setFrequency] = useState<Frequency>(habit?.frequency ?? 'DAILY')
  const [targetCount, setTargetCount] = useState(String(habit?.targetCount ?? 1))
  const [startDate, setStartDate] = useState(habit?.startDate ?? todayIn(user?.timezone ?? 'UTC'))
  const [endDate, setEndDate] = useState(habit?.endDate ?? '')
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const saving = createHabit.isPending || updateHabit.isPending
  const rulesLocked = habit?.rulesLocked ?? false
  const maxTarget = FREQUENCIES.find((f) => f.value === frequency)!.max

  useEffect(() => {
    dialogRef.current?.showModal()
  }, [])

  function chooseFrequency(value: Frequency) {
    setFrequency(value)
    // Keep the target in range: daily is always 1; weekly/monthly default to 1 if the old value doesn't fit.
    const max = FREQUENCIES.find((f) => f.value === value)!.max
    if (Number(targetCount) > max || value === 'DAILY') setTargetCount('1')
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setFieldErrors({})

    const input: HabitInput = {
      name: name.trim(),
      icon: icon.trim() || null,
      points: Number(points),
      frequency,
      targetCount: frequency === 'DAILY' ? 1 : Number(targetCount),
      startDate: startDate || null,
      endDate: endDate || null,
    }

    try {
      if (habit) await updateHabit.mutateAsync({ id: habit.id, input })
      else await createHabit.mutateAsync(input)
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
      aria-labelledby="habit-dialog-title"
      className="m-auto w-[calc(100%-2rem)] max-w-md rounded-2xl bg-slate-900 text-slate-100 border border-slate-800 p-0 backdrop:bg-black/60"
    >
      <form onSubmit={handleSubmit} noValidate className="p-6 space-y-5">
        <h2 id="habit-dialog-title" className="text-xl font-semibold">
          {habit ? 'Edit habit' : 'New habit'}
        </h2>

        {error && (
          <p role="alert" className="text-sm text-red-300 bg-red-950/50 border border-red-900 rounded-lg px-3 py-2">
            {error}
          </p>
        )}

        <Field label="Name" error={fieldErrors.name}>
          <input
            autoFocus
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Eat healthy"
            maxLength={100}
            className={inputClass(fieldErrors.name)}
          />
        </Field>

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
                  icon === emoji ? 'border-emerald-400 bg-emerald-500/15' : 'border-slate-700 hover:border-slate-500'
                }`}
              >
                {emoji}
              </button>
            ))}
          </div>
        </fieldset>

        <Field label="Points per check-in (1–100)" error={fieldErrors.points}>
          <input
            type="number"
            inputMode="numeric"
            min={1}
            max={100}
            value={points}
            onChange={(e) => setPoints(e.target.value)}
            className={inputClass(fieldErrors.points)}
          />
        </Field>

        <fieldset>
          <legend className="text-sm text-slate-300 mb-2">How often</legend>
          <div className="grid grid-cols-3 gap-2" role="radiogroup" aria-disabled={rulesLocked}>
            {FREQUENCIES.map((f) => (
              <button
                key={f.value}
                type="button"
                role="radio"
                aria-checked={frequency === f.value}
                disabled={rulesLocked}
                onClick={() => chooseFrequency(f.value)}
                className={`rounded-lg py-2 text-sm border transition-colors disabled:opacity-60 disabled:cursor-not-allowed ${
                  frequency === f.value
                    ? 'border-emerald-400 bg-emerald-500/15 text-emerald-300'
                    : 'border-slate-700 text-slate-300 hover:border-slate-500'
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>

          {frequency !== 'DAILY' && (
            <label className="flex items-center gap-3 mt-3 text-sm text-slate-300">
              <input
                type="number"
                inputMode="numeric"
                min={1}
                max={maxTarget}
                value={targetCount}
                disabled={rulesLocked}
                onChange={(e) => setTargetCount(e.target.value)}
                className={`${inputClass(fieldErrors.targetCount, 'w-20')} shrink-0 disabled:opacity-60`}
              />
              <span>times per {frequency === 'WEEKLY' ? 'week' : 'month'} (max {maxTarget})</span>
            </label>
          )}

          {rulesLocked && (
            <p className="text-xs text-slate-400 mt-2">
              🔒 Locked after the first check-in, so your streak keeps the rules you started with.
              To change it, archive this habit and create a new one.
            </p>
          )}
        </fieldset>

        <div className="grid grid-cols-2 gap-3">
          <Field label="Start date" error={fieldErrors.startDate}>
            <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className={inputClass(fieldErrors.startDate)} />
          </Field>
          <Field label="End date (optional)" error={fieldErrors.endDate}>
            <input type="date" value={endDate} min={startDate} onChange={(e) => setEndDate(e.target.value)} className={inputClass(fieldErrors.endDate)} />
          </Field>
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg text-slate-300 hover:bg-slate-800">
            Cancel
          </button>
          <button
            type="submit"
            disabled={saving}
            className="px-4 py-2 rounded-lg bg-emerald-500 hover:bg-emerald-400 disabled:opacity-60 text-slate-950 font-semibold"
          >
            {saving ? 'Saving…' : habit ? 'Save changes' : 'Add habit'}
          </button>
        </div>
      </form>
    </dialog>
  )
}

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="text-sm text-slate-300">{label}</span>
      {children}
      {error && <span className="text-xs text-red-300 mt-1 block">{error}</span>}
    </label>
  )
}

function inputClass(error?: string, width = 'mt-1 w-full') {
  return `${width} rounded-lg bg-slate-950 border px-3 py-2 outline-none focus:ring-2 focus:ring-emerald-500 [color-scheme:dark] ${
    error ? 'border-red-500' : 'border-slate-700'
  }`
}
