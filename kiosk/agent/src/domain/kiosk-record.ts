export type GateDirection = 'IN' | 'OUT';

/**
 * Sync status of a locally persisted AttendanceTransaction on KioskEdge.
 * - ONLINE_VERIFIED: Verified by central Backend in real-time and logged locally for audit.
 * - OFFLINE_UNVERIFIED: Backend unreachable; stored in SQLite for Store-and-Forward sync.
 * - SYNCED: Previously OFFLINE_UNVERIFIED, successfully forwarded to Backend by SyncAgent.
 * - SYNC_FAILED: Backend rejected during deferred sync (e.g. expired/replay/invalid signature).
 */
export type KioskSyncStatus =
  | 'ONLINE_VERIFIED'
  | 'OFFLINE_UNVERIFIED'
  | 'SYNCED'
  | 'SYNC_FAILED';

export interface ParsedQrPayload {
  raw: string;
  employeeId: string;
  timestamp: number | null;
  nonce: string | null;
  signature: string | null;
  format: 'JSON_DYNAMIC' | 'DELIMITED_LEGACY' | 'PLAIN_TEXT';
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

export type ScanDecision = 'GRANTED' | 'OFFLINE_STORED' | 'DENIED';

export interface ProcessScanInput {
  qrPayload: string;
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
