import { Html5Qrcode } from 'html5-qrcode';
import { FormEvent, useEffect, useRef, useState } from 'react';

interface CameraDevice {
  id: string;
  label: string;
}

interface QrScannerPanelProps {
  onDecodedQr: (qrText: string) => Promise<void>;
  isBusy: boolean;
}

const SCANNER_REGION_ID = 'tmc-kiosk-qr-reader';
const COOLDOWN_MS = 2500;

export function QrScannerPanel({ onDecodedQr, isBusy }: QrScannerPanelProps) {
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const lastDecodedRef = useRef<{ text: string; at: number }>({ text: '', at: 0 });
  const inFlightRef = useRef(false);
  const onDecodedRef = useRef(onDecodedQr);
  onDecodedRef.current = onDecodedQr;

  const [cameras, setCameras] = useState<CameraDevice[]>([]);
  const [selectedCameraId, setSelectedCameraId] = useState<string>('');
  const [isScanning, setIsScanning] = useState(false);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [manualInput, setManualInput] = useState('');
  const [lastRawQr, setLastRawQr] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    Html5Qrcode.getCameras()
      .then((devices) => {
        if (!active) return;
        const mapped = devices.map((d, idx) => ({
          id: d.id,
          label: d.label || `Camera ${idx + 1}`,
        }));
        setCameras(mapped);
        if (mapped.length > 0) {
          setSelectedCameraId(mapped[0].id);
        }
      })
      .catch((err) => {
        if (!active) return;
        setCameraError(
          err instanceof Error
            ? `Không lấy được danh sách camera: ${err.message}`
            : 'Không thể truy cập webcam.',
        );
      });

    return () => {
      active = false;
      const instance = scannerRef.current;
      if (instance) {
        instance
          .stop()
          .catch(() => undefined)
          .finally(() => {
            instance.clear();
          });
      }
    };
  }, []);

  async function handleDecodedText(decodedText: string) {
    const now = Date.now();
    if (inFlightRef.current) return;
    if (
      lastDecodedRef.current.text === decodedText &&
      now - lastDecodedRef.current.at < COOLDOWN_MS
    ) {
      return;
    }

    lastDecodedRef.current = { text: decodedText, at: now };
    inFlightRef.current = true;
    setLastRawQr(decodedText);
    console.log('[Kiosk Scanner] Decoded QR:', decodedText);

    try {
      await onDecodedRef.current(decodedText);
    } finally {
      inFlightRef.current = false;
    }
  }

  async function startCamera() {
    setCameraError(null);
    if (isScanning) return;

    try {
      const instance = scannerRef.current || new Html5Qrcode(SCANNER_REGION_ID);
      scannerRef.current = instance;

      const cameraConfig = selectedCameraId
        ? selectedCameraId
        : { facingMode: 'environment' };

      await instance.start(
        cameraConfig,
        {
          fps: 10,
          qrbox: { width: 240, height: 240 },
        },
        (decodedText) => {
          void handleDecodedText(decodedText);
        },
        () => {
          // Ignore per-frame decode misses while scanning
        },
      );
      setIsScanning(true);
    } catch (err) {
      setCameraError(
        err instanceof Error
          ? `Không bật được camera: ${err.message}`
          : 'Lỗi khởi động camera.',
      );
    }
  }

  async function stopCamera() {
    const instance = scannerRef.current;
    if (!instance || !isScanning) return;
    try {
      await instance.stop();
      instance.clear();
    } catch {
      // ignore stop errors
    } finally {
      setIsScanning(false);
    }
  }

  async function handleManualSubmit(e: FormEvent) {
    e.preventDefault();
    const trimmed = manualInput.trim();
    if (!trimmed || isBusy) return;
    console.log('[Kiosk Scanner] Manual QR input:', trimmed);
    setLastRawQr(trimmed);
    await onDecodedRef.current(trimmed);
  }

  function fillSampleDynamicQr() {
    const sample = JSON.stringify({
      employeeId: 'EMP001',
      timestamp: Date.now(),
      nonce: crypto.randomUUID(),
      signature: 'demo-hmac-placeholder',
    });
    setManualInput(sample);
  }

  return (
    <section className="card scanner-card">
      <div className="card-header">
        <h2>Webcam QR Scanner (html5-qrcode)</h2>
        <div className="scanner-controls">
          {cameras.length > 0 && (
            <select
              value={selectedCameraId}
              disabled={isScanning}
              onChange={(e) => setSelectedCameraId(e.target.value)}
            >
              {cameras.map((cam) => (
                <option key={cam.id} value={cam.id}>
                  {cam.label}
                </option>
              ))}
            </select>
          )}
          {!isScanning ? (
            <button type="button" className="btn btn-primary" onClick={startCamera}>
              Bật Webcam
            </button>
          ) : (
            <button type="button" className="btn btn-danger" onClick={stopCamera}>
              Tắt Webcam
            </button>
          )}
        </div>
      </div>

      {cameraError && <div className="alert-inline error">{cameraError}</div>}

      <div className="scanner-viewport-wrap">
        <div id={SCANNER_REGION_ID} className="scanner-viewport" />
        {!isScanning && (
          <p className="scanner-hint">
            Nhấn <strong>Bật Webcam</strong> và đưa mã QR vào khung hình để quét.
          </p>
        )}
      </div>

      {lastRawQr && (
        <div className="raw-preview">
          <span>Chuỗi QR vừa đọc:</span>
          <code>{lastRawQr}</code>
        </div>
      )}

      <form className="manual-form" onSubmit={handleManualSubmit}>
        <label htmlFor="manual-qr">Test nhanh không cần webcam (nhập/dán chuỗi QR):</label>
        <div className="manual-row">
          <input
            id="manual-qr"
            type="text"
            value={manualInput}
            onChange={(e) => setManualInput(e.target.value)}
            placeholder='{"employeeId":"EMP001","timestamp":...,"nonce":"...","signature":"..."}'
          />
          <button
            type="button"
            className="btn btn-outline"
            onClick={fillSampleDynamicQr}
          >
            Tạo QR mẫu
          </button>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={isBusy || !manualInput.trim()}
          >
            Quét thử
          </button>
        </div>
      </form>
    </section>
  );
}
