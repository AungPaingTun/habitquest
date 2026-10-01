import { useState } from 'react'
import { AppShell } from '../components/AppShell'
import { HabitDialog } from './HabitDialog'
import { describeFrequency, formatDate, useHabits, useSetArchived, type Habit } from './habitsApi'

type DialogState = { mode: 'closed' } | { mode: 'create' } | { mode: 'edit'; habit: Habit }

export function HabitsPage() {
  const [showArchived, setShowArchived] = useState(false)
  const [dialog, setDialog] = useState<DialogState>({ mode: 'closed' })
  const habits = useHabits(showArchived)
  const setArchived = useSetArchived()

  return (
    <AppShell>
      <div className="flex items-center justify-between gap-4 mb-6">
        <h1 className="text-2xl font-bold">Your habits</h1>
        <button
          onClick={() => setDialog({ mode: 'create' })}
          className="rounded-lg bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-semibold px-4 py-2"
        >
          + Add habit
        </button>
      </div>

      <div className="flex gap-1 mb-4 text-sm" role="tablist">
        {[false, true].map((archived) => (
          <button
            key={String(archived)}
            role="tab"
            aria-selected={showArchived === archived}
            onClick={() => setShowArchived(archived)}
            className={`px-3 py-1.5 rounded-lg ${
              showArchived === archived ? 'bg-slate-800 text-slate-100' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            {archived ? 'Archived' : 'Active'}
          </button>
        ))}
      </div>

      {habits.isPending && <p className="text-slate-400">Loading habits…</p>}

      {habits.isError && (
        <p role="alert" className="text-red-300">
          Couldn't load habits: {habits.error.message}
        </p>
      )}

      {habits.data?.length === 0 && (
        <div className="text-center border border-dashed border-slate-800 rounded-2xl py-14 px-6">
          {showArchived ? (
            <p className="text-slate-400">No archived habits.</p>
          ) : (
            <>
              <p className="text-4xl mb-3">🌱</p>
              <p className="font-medium">No habits yet</p>
              <p className="text-slate-400 text-sm mt-1">Add your first habit and decide how many points it's worth.</p>
            </>
          )}
        </div>
      )}

      <ul className="space-y-3">
        {habits.data?.map((habit) => (
          <li key={habit.id} className="flex items-center gap-4 bg-slate-900 border border-slate-800 rounded-2xl p-4">
            <span className="size-12 shrink-0 grid place-items-center rounded-xl bg-slate-800 text-2xl" aria-hidden>
              {habit.icon ?? '⭐'}
            </span>

            <div className="min-w-0 flex-1">
              <p className="font-medium truncate">{habit.name}</p>
              <p className="text-sm text-slate-400">
                {describeFrequency(habit)}
                {habit.endDate && <> · until {formatDate(habit.endDate)}</>}
              </p>
            </div>

            <span className="shrink-0 rounded-full bg-emerald-500/15 text-emerald-300 text-sm font-semibold px-3 py-1">
              +{habit.points} pts
            </span>

            <div className="shrink-0 flex gap-1 text-sm">
              {!habit.archived && (
                <button
                  onClick={() => setDialog({ mode: 'edit', habit })}
                  className="px-2 py-1 rounded-lg text-slate-300 hover:bg-slate-800"
                >
                  Edit
                </button>
              )}
              <button
                onClick={() => setArchived.mutate({ id: habit.id, archived: !habit.archived })}
                disabled={setArchived.isPending}
                className="px-2 py-1 rounded-lg text-slate-400 hover:bg-slate-800 hover:text-slate-200 disabled:opacity-50"
              >
                {habit.archived ? 'Restore' : 'Archive'}
              </button>
            </div>
          </li>
        ))}
      </ul>

      {dialog.mode !== 'closed' && (
        <HabitDialog
          habit={dialog.mode === 'edit' ? dialog.habit : undefined}
          onClose={() => setDialog({ mode: 'closed' })}
        />
      )}
    </AppShell>
  )
}
