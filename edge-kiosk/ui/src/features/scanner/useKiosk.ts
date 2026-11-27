import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { request } from '../../api/client'

export type Transaction = {
  transactionId: string; scanId: string; employeeId: string; gateId: string; direction: string;
  state: string; verificationMode: string; reason: string; sequence: number; occurredAt: string; updatedAt: string;
}
export type Status = {
  kioskId: string; factoryId: string; gateId: string; direction: string; pendingEvents: number;
  coreConnected: boolean; cacheVersion: number; transactions: Transaction[];
}
export type MockState = { service: string; mode: string; openCount: number; eventCount: number; authorizationCount: number }

export function useKiosk() {
  const queryClient = useQueryClient()
  const refresh = () => { void queryClient.invalidateQueries({ queryKey: ['kiosk'] }) }
  const status = useQuery({ queryKey: ['kiosk', 'status'], queryFn: () => request<Status>('/status'), refetchInterval: 2000 })
  const mocks = useQuery({ queryKey: ['kiosk', 'mocks'], queryFn: () => request<MockState[]>('/demo/mocks'), refetchInterval: 3000 })
  const scan = useMutation({
    mutationFn: (input: { scanId: string; payload: string }) => request<Transaction>('/scans', input), onSuccess: refresh,
  })
  const sample = useMutation({ mutationFn: (kind: string) => request<{ payload: string }>(`/demo/samples/${kind}`) })
  const mode = useMutation({ mutationFn: ({ service, mode }: { service: string; mode: string }) => request<MockState>(`/demo/mocks/${service}`, { mode }), onSuccess: refresh })
  const resolve = useMutation({ mutationFn: ({ id, outcome }: { id: string; outcome: string }) => request<void>(`/demo/transactions/${id}/resolve`, { outcome }), onSuccess: refresh })
  return { status, mocks, scan, sample, mode, resolve }
}
