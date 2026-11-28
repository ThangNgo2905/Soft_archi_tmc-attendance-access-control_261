export type GateDirection = 'IN' | 'OUT';

export type KioskSyncStatus =
  | 'ONLINE_VERIFIED'
  | 'OFFLINE_UNVERIFIED'
  | 'SYNCED'
  | 'SYNC_FAILED';

export type ScanDecision = 'GRANTED' | 'OFFLINE_STORED' | 'DENIED';

export interface GateConfig {
  factoryId: string;
  gateId: string;
  direction: GateDirection;
}

export interface ProcessScanResult {
  decision: ScanDecision;
  transactionId: string;
  employeeId: string | null;
  gateId: string;
  direction: GateDirection;
  syncStatus: KioskSyncStatus | null;
  turnstileOpened: boolean;
  message: string;
  reasonCode: string;
  latencyMs: number;
  scannedAt: string;
}

export interface KioskAttendanceRecord {
  transactionId: string;
  employeeId: string;
  factoryId: string;
  gateId: string;
  direction: GateDirection;
  qrPayload: string;
  nonce: string | null;
  scannedAt: string;
  syncStatus: KioskSyncStatus;
  turnstileOpened: boolean;
  retryCount: number;
  lastError: string | null;
  syncedAt: string | null;
}

export interface AgentStatusResponse {
  ok: boolean;
  backendUrl: string;
  backendReachable: boolean;
  simulatedOffline: boolean;
  turnstileUrl: string;
  sqliteDbPath: string;
  defaultGate: GateConfig;
  counts: Record<KioskSyncStatus, number>;
}

export interface QueueResponse {
  counts: Record<KioskSyncStatus, number>;
  records: KioskAttendanceRecord[];
}

export interface SyncTriggerResponse {
  summary: {
    attempted: number;
    synced: number;
    failed: number;
    stillOffline: number;
  };
  counts: Record<KioskSyncStatus, number>;
}
