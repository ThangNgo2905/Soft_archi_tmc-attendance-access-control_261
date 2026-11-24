import { KioskAttendanceRecord, KioskSyncStatus } from '../types';

interface LocalQueueTableProps {
  counts: Record<KioskSyncStatus, number>;
  records: KioskAttendanceRecord[];
  onSyncNow: () => void;
  isSyncing: boolean;
  syncFeedback: string | null;
}

const STATUS_LABELS: Record<KioskSyncStatus, string> = {
  ONLINE_VERIFIED: 'ONLINE_VERIFIED',
  OFFLINE_UNVERIFIED: 'OFFLINE_UNVERIFIED',
  SYNCED: 'SYNCED',
  SYNC_FAILED: 'SYNC_FAILED',
};

export function LocalQueueTable({
  counts,
  records,
  onSyncNow,
  isSyncing,
  syncFeedback,
}: LocalQueueTableProps) {
  return (
    <section className="card queue-card">
      <div className="card-header">
        <div>
          <h2>Bộ đệm SQLite cục bộ (kiosk.db)</h2>
          <div className="status-counters">
            <span className="badge badge-green">
              Online: {counts.ONLINE_VERIFIED}
            </span>
            <span className="badge badge-amber">
              Chờ đồng bộ (OFFLINE_UNVERIFIED): {counts.OFFLINE_UNVERIFIED}
            </span>
            <span className="badge badge-blue">Đã đồng bộ: {counts.SYNCED}</span>
            <span className="badge badge-red">Lỗi sync: {counts.SYNC_FAILED}</span>
          </div>
        </div>

        <button
          type="button"
          className="btn btn-outline"
          onClick={onSyncNow}
          disabled={isSyncing}
        >
          {isSyncing ? 'Đang đồng bộ...' : 'Đồng bộ ngay (SyncAgent)'}
        </button>
      </div>

      {syncFeedback && <div className="alert-inline info">{syncFeedback}</div>}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Thời gian quét</th>
              <th>Mã NV</th>
              <th>Cổng / Chiều</th>
              <th>Turnstile</th>
              <th>Trạng thái SQLite</th>
              <th>Ghi chú / Lỗi</th>
            </tr>
          </thead>
          <tbody>
            {records.length === 0 ? (
              <tr>
                <td colSpan={6} className="empty-row">
                  Chưa có bản ghi nào trong kiosk.db.
                </td>
              </tr>
            ) : (
              records.map((rec) => (
                <tr key={rec.transactionId}>
                  <td>{new Date(rec.scannedAt).toLocaleString('vi-VN')}</td>
                  <td>
                    <strong>{rec.employeeId}</strong>
                  </td>
                  <td>
                    {rec.gateId} ({rec.direction})
                  </td>
                  <td>{rec.turnstileOpened ? 'OPEN' : 'CLOSED'}</td>
                  <td>
                    <span className={`status-pill ${rec.syncStatus}`}>
                      {STATUS_LABELS[rec.syncStatus]}
                    </span>
                  </td>
                  <td className="error-cell">{rec.lastError || '—'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
