const express = require('express');
const app = express();
app.use(express.json());

// Giả lập cổng xoay vật lý Turnstile Controller
app.post('/api/turnstile/unlock', (req, res) => {
  console.log(`[TURNSTILE MOCK] Mở cổng: ${req.body.gateId} | Mã giao dịch: ${req.body.transactionId}`);
  return res.json({ success: true, status: 'OPEN_CONFIRMED' });
});

// Giả lập hệ thống nhân sự HR/Payroll
app.post('/api/hr/sync', (req, res) => {
  console.log(`[HR MOCK] Đã nhận lô dữ liệu chấm công từ TMC System`);
  return res.json({ success: true, count: req.body.records?.length || 0 });
});

app.listen(4000, () => console.log('Mock Server đang chạy tại port 4000...'));
