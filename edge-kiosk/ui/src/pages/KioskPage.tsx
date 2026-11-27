import { useRef, useState } from 'react'
import {
  Alert, Box, Button, Chip, Container, Divider, FormControl, InputLabel, MenuItem, Paper,
  Select, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Typography,
} from '@mui/material'
import { useKiosk } from '../features/scanner/useKiosk'
import { StatusChip } from '../components/common/StatusChip'

const samples = [
  ['VALID', 'QR hợp lệ'], ['VALID_SECOND_EMPLOYEE', 'QR hợp lệ — nhân viên thứ hai'], ['EXPIRED', 'Đã hết hạn'], ['BAD_SIGNATURE', 'Sai chữ ký'],
  ['WRONG_FACTORY', 'Sai nhà máy'], ['OUTSIDE_SHIFT', 'Ngoài ca'], ['INACTIVE', 'Nhân viên ngừng hoạt động'],
]
const modes: Record<string, string[]> = {
  registry: ['NORMAL', 'DOWN', 'DENY', 'LOST_ACK'],
  turnstile: ['NORMAL', 'DOWN', 'DENY', 'LOST_ACK', 'UNKNOWN'],
  core: ['NORMAL', 'DOWN', 'LOST_ACK', 'PARTIAL_ACK'],
}

export function KioskPage() {
  const kiosk = useKiosk()
  const [payload, setPayload] = useState('')
  const [kind, setKind] = useState('VALID')
  // A transport retry reuses the same scanId; editing the payload starts a new request.
  const attempt = useRef<{ payload: string; scanId: string } | null>(null)
  const data = kiosk.status.data
  const errors = [kiosk.scan.error, kiosk.sample.error, kiosk.mode.error, kiosk.resolve.error].filter(Boolean)
  async function loadSample() {
    try {
      const result = await kiosk.sample.mutateAsync(kind)
      setPayload(result.payload)
      attempt.current = null
      kiosk.scan.reset()
    } catch { /* Mutation renders its error below. */ }
  }
  function submit() {
    if (!attempt.current || attempt.current.payload !== payload) attempt.current = { payload, scanId: crypto.randomUUID() }
    kiosk.scan.mutate(attempt.current)
  }
  return <Container maxWidth="lg" sx={{ py: { xs: 3, md: 5 } }}>
    <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
      <Box><Typography variant="overline" color="primary">TMC MANUFACTURING · EDGE</Typography><Typography variant="h4" component="h1">Trạm quét ra / vào</Typography></Box>
      <Chip label="DEMO · dữ liệu mẫu" variant="outlined" />
    </Stack>
    {kiosk.status.isError && <Alert severity="error" sx={{ mb: 2 }}>Không kết nối được ứng dụng Edge. Kiểm tra tiến trình tại máy kiosk.</Alert>}
    <Stack direction="row" gap={1} flexWrap="wrap" mb={3}>
      <Chip label={`Kiosk: ${data?.kioskId ?? '—'}`} /><Chip label={`Nhà máy: ${data?.factoryId ?? '—'}`} />
      <Chip label={`Cổng: ${data?.gateId ?? '—'} · ${data?.direction ?? '—'}`} />
      <Chip color={data?.coreConnected ? 'success' : 'warning'} label={data?.coreConnected ? 'Core đã kết nối' : 'Core chưa kết nối'} />
      <Chip label={`${data?.pendingEvents ?? 0} sự kiện chờ ACK`} />
    </Stack>
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1.4fr 1fr' }, gap: 3 }}>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>Scanner mô phỏng</Typography>
        <Typography color="text.secondary" mb={2}>Dán payload hoặc tạo QR mẫu. Mỗi lần tạo mẫu hợp lệ có nonce và thời hạn mới.</Typography>
        <Stack direction="row" gap={1} mb={2}>
          <FormControl fullWidth size="small"><InputLabel id="sample-label">Loại QR mẫu</InputLabel><Select labelId="sample-label" label="Loại QR mẫu" value={kind} onChange={e => setKind(e.target.value)}>{samples.map(([value, label]) => <MenuItem key={value} value={value}>{label}</MenuItem>)}</Select></FormControl>
          <Button variant="outlined" sx={{ minWidth: 120 }} disabled={kiosk.sample.isPending} onClick={() => void loadSample()}>Tạo mẫu</Button>
        </Stack>
        <TextField label="Payload QR" multiline minRows={4} fullWidth value={payload} inputProps={{ maxLength: 2048 }} onChange={e => { setPayload(e.target.value); attempt.current = null; kiosk.scan.reset() }} />
        <Button fullWidth variant="contained" size="large" sx={{ mt: 2 }} onClick={submit} disabled={!payload.trim() || kiosk.scan.isPending || kiosk.scan.isSuccess}>
          {kiosk.scan.isPending ? 'Đang xử lý…' : kiosk.scan.isSuccess ? 'Đã xử lý — tạo mẫu mới để quét tiếp' : kiosk.scan.isError ? 'Thử lại cùng mã yêu cầu' : 'Quét QR'}
        </Button>
        {errors.map((error, index) => <Alert key={index} severity="error" sx={{ mt: 2 }}>{error?.message}</Alert>)}
        {kiosk.scan.data && <Box mt={2}><StatusChip state={kiosk.scan.data.state} /><Typography sx={{ mt: 1 }}>{kiosk.scan.data.reason}</Typography></Box>}
      </Paper>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>Dịch vụ mô phỏng</Typography>
        <Typography color="text.secondary" mb={2}>DOWN: mất kết nối. LOST_ACK: dịch vụ đã xử lý nhưng phản hồi bị trễ.</Typography>
        <Stack spacing={2}>{kiosk.mocks.data?.map(mock => <Box key={mock.service}>
          <Stack direction="row" gap={1} alignItems="center" justifyContent="space-between">
            <Typography sx={{ textTransform: 'capitalize' }}>{mock.service}</Typography>
            <FormControl size="small" sx={{ minWidth: 155 }}><InputLabel id={`${mock.service}-label`}>Chế độ</InputLabel><Select labelId={`${mock.service}-label`} label="Chế độ" value={mock.mode === 'UNREACHABLE' ? '' : mock.mode} disabled={kiosk.mode.isPending || mock.mode === 'UNREACHABLE'} onChange={e => kiosk.mode.mutate({ service: mock.service, mode: e.target.value })}>{modes[mock.service]?.map(mode => <MenuItem key={mode} value={mode}>{mode}</MenuItem>)}</Select></FormControl>
          </Stack>
          <Typography variant="caption" color="text.secondary">{mock.mode === 'UNREACHABLE' ? 'Mock chưa khởi động' : `${mock.openCount} lần mở · ${mock.eventCount} sự kiện tại Core`}</Typography>
        </Box>)}</Stack>
        {kiosk.mocks.isError && <Alert severity="warning">Không đọc được trạng thái mock.</Alert>}
        <Divider sx={{ my: 2 }} /><Typography variant="body2">Sau một lượt IN thành công, IN tiếp sẽ bị anti-passback. Dùng kiosk OUT cùng Registry để thử chiều ngược lại.</Typography>
      </Paper>
    </Box>
    <Typography variant="h6" mt={4} mb={2}>Giao dịch gần đây</Typography>
    <TableContainer component={Paper}><Table size="small" aria-label="Giao dịch kiosk"><TableHead><TableRow>{['Thời gian', 'Nhân viên', 'Chiều', 'Kết quả', 'Xác minh', 'Lý do / xử lý'].map(label => <TableCell key={label}>{label}</TableCell>)}</TableRow></TableHead>
      <TableBody>{data?.transactions.map(tx => <TableRow key={tx.transactionId}>
        <TableCell>{new Date(tx.occurredAt).toLocaleTimeString('vi-VN')}</TableCell><TableCell>{tx.employeeId}</TableCell><TableCell>{tx.direction}</TableCell><TableCell><StatusChip state={tx.state} /></TableCell>
        <TableCell><Chip size="small" variant="outlined" color={tx.verificationMode === 'OFFLINE_UNVERIFIED' ? 'warning' : 'default'} label={tx.verificationMode} /></TableCell>
        <TableCell>{tx.state === 'OPEN_UNKNOWN' ? <Stack gap={1}><Typography variant="caption">Mô phỏng xác nhận cảm biến:</Typography><Stack direction="row" gap={1}><Button size="small" disabled={kiosk.resolve.isPending} onClick={() => kiosk.resolve.mutate({ id: tx.transactionId, outcome: 'OPEN_CONFIRMED' })}>Đã mở</Button><Button size="small" disabled={kiosk.resolve.isPending} onClick={() => kiosk.resolve.mutate({ id: tx.transactionId, outcome: 'DENIED' })}>Không mở</Button></Stack></Stack> : tx.reason}</TableCell>
      </TableRow>)}{!data?.transactions.length && <TableRow><TableCell colSpan={6}>Chưa có giao dịch. Tạo QR mẫu để bắt đầu.</TableCell></TableRow>}</TableBody>
    </Table></TableContainer>
  </Container>
}
