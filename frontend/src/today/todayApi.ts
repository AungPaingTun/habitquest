import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { Frequency } from '../habits/habitsApi'
import { analyticsKey } from '../analytics/analyticsApi'
import { api } from '../lib/api'
import { pointsKey, type PointsSummary } from '../points/pointsApi'

export type HabitStatus = 'UPCOMING' | 'ACTIVE' | 'COMPLETED'

export type TodayHabit = {
  id: number
  name: string
  icon: string | null
  points: number
  frequency: Frequency
  targetCount: number
  startDate: string
  endDate: string | null
  status: HabitStatus
  doneToday: boolean
  periodCount: number
  currentStreak: number
  bestStreak: number
  canCheckIn: boolean
}

export type TodayResponse = {
  date: string
  points: PointsSummary
  habits: TodayHabit[]
}

export type CheckInResult = {
  habitId: number
  pointsChange: number
  bonus: number
  currentStreak: number
  points: PointsSummary
}

export const todayKey = ['today'] as const

export function useToday() {
  return useQuery({ queryKey: todayKey, queryFn: () => api<TodayResponse>('/today') })
}

/** Check-in and undo change both the Today list and the points, so refresh both afterwards. */
function useCheckInMutation(method: 'POST' | 'DELETE') {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (habitId: number) => api<CheckInResult>(`/habits/${habitId}/check-in`, { method }),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: todayKey })
      queryClient.invalidateQueries({ queryKey: pointsKey })
      queryClient.invalidateQueries({ queryKey: analyticsKey })
    },
  })
}

export function useCheckIn() {
  return useCheckInMutation('POST')
}

export function useUndoCheckIn() {
  return useCheckInMutation('DELETE')
}
