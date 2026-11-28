import { useCallback, useEffect, useState } from 'react';
import { kioskAgentClient } from './api/kioskAgentClient';
import { GateConfigBar } from './components/GateConfigBar';
import { LocalQueueTable } from './components/LocalQueueTable';
import { QrScannerPanel } from './components/QrScannerPanel';
import { ScanResultBanner } from './components/ScanResultBanner';
import {
  AgentStatusResponse,
  GateConfig,
  KioskAttendanceRecord,
  KioskSyncStatus,
  ProcessScanResult,
} from './types';

const INITIAL_COUNTS: Record<KioskSyncStatus, number> = {
  ONLINE_VERIFIED: 0,
  OFFLINE_UNVERIFIED: 0,
  SYNCED: 0,
  SYNC_FAILED: 0,
};

export function App() {
  const [gate, setGate] = useState<GateConfig>({
    factoryId: 'Binh Duong',
    gateId: 'GATE_BD_IN_01',
    direction: 'IN',
  });
  const [agentStatus, setAgentStatus] = useState<AgentStatusResponse | null>(null);
  const [counts, setCounts] = useState<Record<KioskSyncStatus, number>>(INITIAL_COUNTS);
  const [records, setRecords] = useState<KioskAttendanceRecord[]>([]);
  const [scanResult, setScanResult] = useState<ProcessScanResult | null>(null);
  const [agentError, setAgentError] = useState<string | null>(null);
  const [isBusy, setIsBusy] = useState(false);
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncFeedback, setSyncFeedback] = useState<string | null>(null);

  const refreshAgentData = useCallback(async () => {
    try {
      const [status, queue] = await Promise.all([
        kioskAgentClient.getStatus(),
        kioskAgentClient.getQueue(30),
      ]);
      setAgentStatus(status);
      setCounts(queue.counts);
      setRecords(queue.records);
      setAgentError(null);
    } catch {
      setAgentError(
        `Không kết nối được tới Local Kiosk Agent (${kioskAgentClient.baseUrl}). Hãy chạy kiosk/agent trước.`,
      );
    }
  }, []);

  useEffect(() => {
    void refreshAgentData();
    const timer = setInterval(() => {
      void refreshAgentData();
    }, 3000);
    return () => clearInterval(timer);
  }, [refreshAgentData]);

  async function handleDecodedQr(qrPayload: string) {
    setIsBusy(true);
    try {
      const result = await kioskAgentClient.submitScan(qrPayload, gate);
      setScanResult(result);
      setAgentError(null);
      await refreshAgentData();
    } catch {
      setAgentError(
        `Không gửi được dữ liệu quét tới Kiosk Agent (${kioskAgentClient.baseUrl}).`,
      );
    } finally {
      setIsBusy(false);
    }
  }

  async function handleToggleOffline() {
    if (!agentStatus) return;
    try {
      await kioskAgentClient.setSimulatedOffline(!agentStatus.simulatedOffline);
      await refreshAgentData();
    } catch {
      setAgentError('Không chuyển được chế độ giả lập offline.');
    }
  }

  async function handleSyncNow() {
    setIsSyncing(true);
    setSyncFeedback(null);
    try {
      const res = await kioskAgentClient.triggerSync();
      setSyncFeedback(
        `Đã kiểm tra ${res.summary.attempted} bản ghi: thành công ${res.summary.synced}, lỗi ${res.summary.failed}, còn offline ${res.summary.stillOffline}.`,
      );
      await refreshAgentData();
    } catch {
      setSyncFeedback('Gọi đồng bộ thất bại — kiểm tra kết nối Kiosk Agent.');
    } finally {
      setIsSyncing(false);
    }
  }

  const isOnline = Boolean(agentStatus?.backendReachable && !agentStatus?.simulatedOffline);

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <h1>TMC Edge Kiosk — Gate Scanner</h1>
          <p className="subtitle">
            {gate.factoryId} • {gate.gateId} ({gate.direction})
          </p>
        </div>

        <div className={`connection-pill ${isOnline ? 'online' : 'offline'}`}>
          <span className="dot" />
          {agentStatus
            ? isOnline
              ? 'ONLINE (Backend Connected)'
              : 'OFFLINE (Store-and-Forward SQLite)'
            : 'CONNECTING AGENT...'}
        </div>
      </header>

      <main className="main-grid">
        <GateConfigBar
          gate={gate}
          onChange={setGate}
          simulatedOffline={Boolean(agentStatus?.simulatedOffline)}
          onToggleOffline={handleToggleOffline}
        />

        <ScanResultBanner result={scanResult} error={agentError} />

        <QrScannerPanel onDecodedQr={handleDecodedQr} isBusy={isBusy} />

        <LocalQueueTable
          counts={counts}
          records={records}
          onSyncNow={handleSyncNow}
          isSyncing={isSyncing}
          syncFeedback={syncFeedback}
        />
      </main>
    </div>
  );
}

export default App;
