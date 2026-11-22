import { Alert, Chip, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { fetchMeta } from '../api/meta'
import { getApiBaseUrl } from '../api/client'
import { PageHeader } from '../components/common/PageHeader'

export function DashboardPage() {
  const metaQuery = useQuery({
    queryKey: ['meta'],
    queryFn: fetchMeta,
    retry: false,
  })

  return (
    <>
      <PageHeader
        title="Dashboard"
        subtitle="TMC Manufacturing — Binh Duong & Long An factories"
      />
      <Stack spacing={2}>
        <Typography variant="body1">
          Web portal scaffold for QR attendance and gate access control.
        </Typography>
        <Typography variant="body2" color="text.secondary">
          API base: {getApiBaseUrl()}
        </Typography>
        {metaQuery.isLoading ? <Alert severity="info">Loading API metadata…</Alert> : null}
        {metaQuery.isError ? (
          <Alert severity="warning">
            Backend not reachable yet. Start Compose or run the API on port 8080.
          </Alert>
        ) : null}
        {metaQuery.data ? (
          <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
            <Chip label={metaQuery.data.data.name} color="primary" />
            <Chip label={metaQuery.data.data.version} />
            <Chip label={metaQuery.data.data.phase} color="secondary" />
          </Stack>
        ) : null}
      </Stack>
    </>
  )
}
