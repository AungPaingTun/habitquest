import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { todayKey } from '../today/todayApi'

export type Frequency = 'DAILY' | 'WEEKLY' | 'MONTHLY'

export type Habit = {
  id: number
  name: string
  icon: string | null
  points: number
  frequency: Frequency
  targetCount: number
  startDate: string // "YYYY-MM-DD"
  endDate: string | null
  archived: boolean
  /** True after the first check-in: frequency and target can't change anymore. */
  rulesLocked: boolean
  createdAt: string
}

/** Body for POST /habits and PUT /habits/{id}. Matches the backend's HabitRequest. */
export type HabitInput = {
  name: string
  icon: string | null
  points: number
  frequency: Frequency
  targetCount: number
  startDate: string | null
  endDate: string | null
}

const habitsKey = ['habits'] as const

export function useHabits(archived: boolean) {
  return useQuery({
    queryKey: [...habitsKey, { archived }],
    queryFn: () => api<Habit[]>(`/habits?archived=${archived}`),
  })
}

/** After any change, refetch the habit lists (active and archived) and the Today view so every screen stays in sync. */
function useHabitMutation<TVariables>(request: (variables: TVariables) => Promise<Habit>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: request,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: habitsKey })
      queryClient.invalidateQueries({ queryKey: todayKey })
    },
  })
}

export function useCreateHabit() {
  return useHabitMutation((input: HabitInput) => api<Habit>('/habits', { method: 'POST', body: input }))
}

export function useUpdateHabit() {
  return useHabitMutation(({ id, input }: { id: number; input: HabitInput }) =>
    api<Habit>(`/habits/${id}`, { method: 'PUT', body: input }))
}

export function useSetArchived() {
  return useHabitMutation(({ id, archived }: { id: number; archived: boolean }) =>
    api<Habit>(`/habits/${id}/${archived ? 'archive' : 'restore'}`, { method: 'POST' }))
}

/** "Daily", "2× per week", "Once a month" … */
export function describeFrequency(habit: Pick<Habit, 'frequency' | 'targetCount'>): string {
  if (habit.frequency === 'DAILY') return 'Daily'
  const period = habit.frequency === 'WEEKLY' ? 'week' : 'month'
  return habit.targetCount === 1 ? `Once a ${period}` : `${habit.targetCount}× per ${period}`
}

/** "Oct 31, 2026" from "2026-10-31", without time-zone shifts. */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(year, month - 1, day).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
}

/** Today's date as "YYYY-MM-DD" in the given IANA time zone. */
export function todayIn(timeZone: string): string {
  // en-CA formats dates as YYYY-MM-DD.
  return new Intl.DateTimeFormat('en-CA', { timeZone }).format(new Date())
}
