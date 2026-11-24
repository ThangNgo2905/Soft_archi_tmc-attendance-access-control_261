import {
  GateDirection,
  KioskAttendanceRecord,
  KioskSyncStatus,
} from './kiosk-record';

/**
 * Port for local SQLite persistence (Store-and-Forward buffer on KioskEdge).
 */
export interface IKioskQueueRepository {
  saveRecord(record: KioskAttendanceRecord): void;
  hasRecentNonce(nonce: string): boolean;
  findPendingSync(limit?: number): KioskAttendanceRecord[];
  findRecent(limit?: number): KioskAttendanceRecord[];
  countByStatus(): Record<KioskSyncStatus, number>;
  markSynced(transactionId: string, syncedAt: string): void;
  markSyncFailed(transactionId: string, errorMessage: string): void;
  incrementRetry(transactionId: string, errorMessage: string): void;
}

export interface BackendCheckInPayload {
  transactionId: string;
  employeeId: string;
  factoryId: string;
  gateId: string;
  direction: GateDirection;
  qrPayload: string;
  scannedAt: string;
  offlineSync?: boolean;
}

export type BackendCallOutcome =
  | {
      kind: 'ACCEPTED';
      turnstileOpened: boolean;
      message: string;
    }
  | {
      kind: 'REJECTED';
      reasonCode: string;
      message: string;
    }
  | {
      kind: 'UNREACHABLE';
      error: string;
    };

/**
 * Port for communicating with the central TMC Modular Monolith Backend.
 */
export interface IBackendGateway {
  checkIn(payload: BackendCheckInPayload): Promise<BackendCallOutcome>;
  syncOfflineRecord(payload: BackendCheckInPayload): Promise<BackendCallOutcome>;
  checkHealth(): Promise<boolean>;
  isSimulatedOffline(): boolean;
  setSimulatedOffline(offline: boolean): void;
}

/**
 * Port for commanding the physical Turnstile Controller API (or Mock Server).
 */
export interface ITurnstileGateway {
  unlockGate(
    gateId: string,
    transactionId: string,
  ): Promise<{ success: boolean; status: string; error?: string }>;
}
