import { ProcessScanResult } from '../types';

interface ScanResultBannerProps {
  result: ProcessScanResult | null;
  error: string | null;
}

export function ScanResultBanner({ result, error }: ScanResultBannerProps) {
  if (error) {
    return (
      <section className="result-banner denied">
        <div className="result-title">LỖI KẾT NỐI KIOSK AGENT</div>
        <p>{error}</p>
      </section>
    );
  }

  if (!result) {
    return (
      <section className="result-banner idle">
        <div className="result-title">SẴN SÀNG QUÉT MÃ QR</div>
        <p>Đưa mã QR động trên ứng dụng nhân viên vào trước camera.</p>
      </section>
    );
  }

  const classMap = {
    GRANTED: 'granted',
    OFFLINE_STORED: 'offline',
    DENIED: 'denied',
  } as const;

  const labelMap = {
    GRANTED: 'HỢP LỆ — ĐÃ MỞ CỔNG',
    OFFLINE_STORED: 'OFFLINE — LƯU TẠM SQLITE & MỞ CỔNG',
    DENIED: 'TỪ CHỐI MỞ CỔNG',
  } as const;

  return (
    <section className={`result-banner ${classMap[result.decision]}`}>
      <div className="result-top">
        <span className="result-title">{labelMap[result.decision]}</span>
        <span className="latency-pill">{result.latencyMs} ms</span>
      </div>
      <p className="result-message">{result.message}</p>
      <div className="result-meta">
        <span>
          <strong>Nhân viên:</strong> {result.employeeId || 'N/A'}
        </span>
        <span>
          <strong>Cổng:</strong> {result.gateId} ({result.direction})
        </span>
        <span>
          <strong>Turnstile:</strong>{' '}
          {result.turnstileOpened ? 'ĐÃ KÍCH HOẠT MỞ' : 'ĐANG ĐÓNG'}
        </span>
        <span>
          <strong>Mã trạng thái:</strong> {result.reasonCode}
        </span>
      </div>
    </section>
  );
}
