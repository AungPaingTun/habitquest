import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'

export type PointsSummary = {
  balance: number
  lifetimeXp: number
  level: number
  levelStartXp: number
  nextLevelXp: number
}

export type Transaction = {
  id: number
  amount: number
  type: 'EARN' | 'STREAK_BONUS' | 'UNDO' | 'REDEEM'
  description: string
  createdAt: string
}

export const pointsKey = ['points'] as const

export function usePointsSummary() {
  return useQuery({
    queryKey: [...pointsKey, 'summary'],
    queryFn: () => api<PointsSummary>('/points/summary'),
  })
}

/** 0–100: how far the user is from the start of their level to the next one. */
export function levelProgress(p: PointsSummary): number {
  const span = p.nextLevelXp - p.levelStartXp
  return span > 0 ? Math.min(100, Math.round(((p.lifetimeXp - p.levelStartXp) / span) * 100)) : 0
}
