import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { analyticsKey } from '../analytics/analyticsApi'
import { api } from '../lib/api'
import { pointsKey, type PointsSummary } from '../points/pointsApi'
import { todayKey } from '../today/todayApi'

export type Prize = {
  id: number
  name: string
  icon: string | null
  cost: number
  archived: boolean
  redeemCount: number
  lastRedeemedAt: string | null
  createdAt: string
}

export type RedeemResult = { prize: Prize; points: PointsSummary }

const prizesKey = ['prizes'] as const

export function usePrizes(archived: boolean) {
  return useQuery({
    queryKey: [...prizesKey, { archived }],
    queryFn: () => api<Prize[]>(`/prizes?archived=${archived}`),
  })
}

function usePrizeMutation<TVariables, TResult>(request: (variables: TVariables) => Promise<TResult>, affectsPoints = false) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: request,
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: prizesKey })
      if (affectsPoints) {
        queryClient.invalidateQueries({ queryKey: pointsKey })
        queryClient.invalidateQueries({ queryKey: todayKey })
        queryClient.invalidateQueries({ queryKey: analyticsKey })
      }
    },
  })
}

export function useCreatePrize() {
  return usePrizeMutation((input: { name: string; icon: string | null; cost: number }) =>
    api<Prize>('/prizes', { method: 'POST', body: input }))
}

/** Only name and icon: the cost is locked once a prize is created. */
export function useUpdatePrize() {
  return usePrizeMutation(({ id, name, icon }: { id: number; name: string; icon: string | null }) =>
    api<Prize>(`/prizes/${id}`, { method: 'PUT', body: { name, icon } }))
}

export function useSetPrizeArchived() {
  return usePrizeMutation(({ id, archived }: { id: number; archived: boolean }) =>
    api<Prize>(`/prizes/${id}/${archived ? 'archive' : 'restore'}`, { method: 'POST' }))
}

export function useRedeemPrize() {
  return usePrizeMutation((id: number) => api<RedeemResult>(`/prizes/${id}/redeem`, { method: 'POST' }), true)
}
