# TMC Edge Kiosk (`kiosk/web` + `kiosk/agent`)

Phần Edge Kiosk của hệ thống điểm danh & kiểm soát ra/vào TMC gồm 2 thành phần chạy cục bộ tại mỗi trạm cổng (`GateStation`):

1. **`kiosk/web`** (React + Vite + `html5-qrcode`, mặc định port `5173`):
   - Màn hình web đọc mã QR động qua webcam (có cooldown 2.5s chống quét lặp frame, hỗ trợ chọn camera và ô test nhanh không cần webcam).
   - Hiển thị trạng thái kết nối (`ONLINE` / `OFFLINE`), kết quả mở cổng (`GRANTED` / `OFFLINE_STORED` / `DENIED`), độ trễ phản hồi (`ms`) và danh sách bản ghi trong `kiosk.db`.
2. **`kiosk/agent`** (Node.js + TypeScript + Express + `better-sqlite3`, mặc định port `3001`):
   - Lưu bộ đệm ngoại tuyến vào SQLite (`kiosk.db`, bảng `local_attendance_logs`) theo cơ chế **Store-and-Forward**.
   - Khi Backend trung tâm mất kết nối/timeout, Agent vẫn kiểm tra định dạng/HMAC tại Edge (nếu cấu hình `EDGE_HMAC_SECRET`), lưu bản ghi với cờ `OFFLINE_UNVERIFIED`, và gọi Turnstile (`http://localhost:4000/api/turnstile/unlock`) để mở cổng dự phòng.
   - Tiến trình nền **`SyncAgent`** định kỳ kiểm tra và đẩy các bản ghi `OFFLINE_UNVERIFIED` lên Backend khi có mạng trở lại, chuyển trạng thái thành `SYNCED` (hoặc `SYNC_FAILED` nếu Backend từ chối).

---

## 1. Hướng dẫn chạy Kiosk tại local

Mở **3 terminal riêng** (chạy từ thư mục gốc repository):

### Terminal 1 — Mock Turnstile & HR Server (cổng `4000`)
```bash
cd mock-servers
npm install
npm start
```

### Terminal 2 — Local Kiosk Agent + SQLite (cổng `3001`)
```bash
cd kiosk/agent
cp .env.example .env   # tuỳ chỉnh nếu đổi port/secret
npm install
npm run dev
```

### Terminal 3 — Kiosk Web Scanner (cổng `5173`)
```bash
cd kiosk/web
cp .env.example .env
npm install
npm run dev
```

Sau đó mở trình duyệt tại `http://localhost:5173`.

### Chạy test tự động cho Store-and-Forward & QR Edge Validation
```bash
cd kiosk/agent
npm test
```

---

## 2. Hướng dẫn & Contract tích hợp cho Nhóm Mobile Web (Frontend - Nhóm 4)

Để mã QR sinh trên ứng dụng Mobile Web đọc và xác thực được với Kiosk & Backend, vui lòng sinh chuỗi QR theo đúng chuẩn JSON dưới đây (mỗi 30 giây sinh mới 1 lần):

```json
{
  "employeeId": "EMP001",
  "timestamp": 1732291200000,
  "nonce": "550e8400-e29b-41d4-a716-446655440000",
  "signature": "<hmac_sha256_hex>"
}
```

### Quy ước tính `signature` (HMAC-SHA256)
- **Chuỗi đầu vào ký (message)**: `` `${employeeId}:${timestamp}:${nonce}` ``
- **Thuật toán**: `HMAC-SHA256`, xuất chuỗi `hex` (chữ thường).
- **Ví dụ với `crypto-js`**:
  ```ts
  import CryptoJS from 'crypto-js';

  const timestamp = Date.now();
  const nonce = crypto.randomUUID();
  const message = `${employeeId}:${timestamp}:${nonce}`;
  const signature = CryptoJS.HmacSHA256(message, QR_HMAC_SECRET).toString(CryptoJS.enc.Hex);

  const qrValue = JSON.stringify({ employeeId, timestamp, nonce, signature });
  ```
- Lưu ý: Khi test cả hệ thống ở chế độ Offline có bật xác thực chữ ký tại Edge, cần đặt `EDGE_HMAC_SECRET` trong `kiosk/agent/.env` trùng với `QR_HMAC_SECRET` của Mobile Web & Backend.

---

## 3. Hướng dẫn & Contract API cho Nhóm Backend Core (Nhóm 2)

`kiosk/agent` mặc định gọi tới `BACKEND_URL=http://localhost:3000` (có thể đổi trong `kiosk/agent/.env`). Backend cần cung cấp **3 endpoint** sau:

### 3.1. `GET /health`
- Dùng cho `SyncAgent` và Kiosk UI kiểm tra trạng thái kết nối Backend.
- Response thành công: HTTP `200` (ví dụ `{ "ok": true }`).

### 3.2. `POST /api/attendance/check-in`
Kiosk Agent gửi lên khi quét QR ở chế độ Online.

**Request Body:**
```json
{
  "transactionId": "uuid-v4",
  "employeeId": "EMP001",
  "factoryId": "Binh Duong",
  "gateId": "GATE_BD_IN_01",
  "direction": "IN",
  "qrPayload": "{\"employeeId\":\"EMP001\",...}",
  "scannedAt": "2026-11-22T16:20:00.000Z"
}
```

**Response mong đợi từ Backend:**
- **Hợp lệ (HTTP `200` hoặc `201`)**:
  ```json
  {
    "turnstileOpened": true,
    "message": "Chào mừng EMP001 — Check-in thành công."
  }
  ```
  *(Lưu ý: Nếu Backend đã tự gọi `ITurnstileCommandPort.unlockGate(...)` tới `localhost:4000`, hãy trả `"turnstileOpened": true` để Kiosk không gọi mở cổng lần 2. Nếu trả `false` hoặc bỏ trống, Kiosk Agent sẽ tự gọi `http://localhost:4000/api/turnstile/unlock`.)*

- **Từ chối nghiệp vụ (HTTP `400` / `401` / `403` / `409` / `422`)**:
  ```json
  {
    "reasonCode": "QR_EXPIRED",
    "message": "Mã QR đã hết hạn (quá 30 giây)."
  }
  ```
  Các mã gợi ý: `QR_EXPIRED`, `QR_INVALID_SIGNATURE`, `REPLAY_NONCE_DETECTED`, `ANTI_PASSBACK_VIOLATION`, `OUT_OF_SHIFT`.
  *(Khi gặp mã 4xx này, Kiosk sẽ **không** lưu sang hàng đợi offline và hiển thị màn hình từ chối đỏ.)*

- **Lỗi hệ thống / Mất mạng (HTTP `5xx` hoặc timeout > `1500ms`)**:
  Kiosk Agent tự động chuyển sang **Store-and-Forward**, lưu bản ghi vào `kiosk.db` với `sync_status = 'OFFLINE_UNVERIFIED'` và gọi Turnstile mở cổng dự phòng.

### 3.3. `POST /api/attendance/sync`
Được `SyncAgent` gọi tự động khi Backend hoạt động trở lại để đẩy bù các bản ghi `OFFLINE_UNVERIFIED`.

**Request Body:** Giống `POST /api/attendance/check-in`, kèm trường `"offlineSync": true`:
```json
{
  "transactionId": "uuid-v4-same-as-offline",
  "employeeId": "EMP001",
  "factoryId": "Binh Duong",
  "gateId": "GATE_BD_IN_01",
  "direction": "IN",
  "qrPayload": "{\"employeeId\":\"EMP001\",...}",
  "scannedAt": "2026-11-22T16:20:00.000Z",
  "offlineSync": true
}
```

**Lưu ý quan trọng cho Backend khi xử lý `/api/attendance/sync`:**
1. **Idempotency theo `transactionId`**: Nếu `SyncAgent` gửi lại cùng `transactionId` đã từng lưu, Backend cần bỏ qua ghi trùng và trả HTTP `200`.
2. **Không dùng thời gian server hiện tại (`Date.now()`) để chặn TTL 30s cho bản ghi `offlineSync: true`**: Cần đối chiếu `timestamp` bên trong `qrPayload` với `scannedAt` (thời điểm quét tại Kiosk) thay vì thời điểm đồng bộ muộn, đồng thời lưu `scannedAt` vào bảng Append-Only `attendance_transactions`.

---

## 4. Kiến trúc nội bộ `kiosk/agent` (Ports & Adapters / SOLID)

Cấu trúc `kiosk/agent/src/` tuân thủ **Clean Architecture** khớp với `backend/src/modules/attendance/ports/`:
- `domain/ports.ts`: Định nghĩa các interface `IKioskQueueRepository`, `IBackendGateway`, `ITurnstileGateway` (áp dụng DIP & ISP).
- `domain/qr-parser.ts`: Tách riêng hàm thuần `parseQrPayload`, `computeQrHmacSignature`, `validateQrAtEdge`.
- `application/process-scan.usecase.ts`: Luồng xử lý quét thẻ Online → Fallback Offline Store-and-Forward.
- `application/sync-agent.ts`: Tiến trình đồng bộ ngầm `OFFLINE_UNVERIFIED` → `SYNCED`.
- `adapters/`: `SqliteQueueRepository` (`better-sqlite3`), `HttpBackendGateway`, `HttpTurnstileGateway`.
