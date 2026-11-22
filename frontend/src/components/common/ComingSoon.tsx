import { Alert, Stack, Typography } from '@mui/material'

type Props = {
  area: string
  actors?: string
}

export function ComingSoon({ area, actors }: Props) {
  return (
    <Stack spacing={2}>
      <Alert severity="info">Coming soon — structure scaffold only.</Alert>
      <Typography variant="body1">
        Feature area: <strong>{area}</strong>
      </Typography>
      {actors ? (
        <Typography variant="body2" color="text.secondary">
          Intended actors: {actors}
        </Typography>
      ) : null}
    </Stack>
  )
}
