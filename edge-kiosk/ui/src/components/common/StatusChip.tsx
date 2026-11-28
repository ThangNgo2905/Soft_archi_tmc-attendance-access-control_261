import { Chip } from '@mui/material'

export function StatusChip({ state }: { state: string }) {
  const color = state === 'OPEN_CONFIRMED' ? 'success' : state === 'DENIED' ? 'error' : 'warning'
  return <Chip size="small" label={state} color={color} />
}
