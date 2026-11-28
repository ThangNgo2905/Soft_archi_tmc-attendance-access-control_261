import Database from 'better-sqlite3';
import {
  GateDirection,
  KioskAttendanceRecord,
  KioskSyncStatus,
} from '../domain/kiosk-record';
import { IKioskQueueRepository } from '../domain/ports';

interface SqliteRow {
  transaction_id: string;
  employee_id: string;
  factory_id: string;
  gate_id: string;
  direction: string;
  qr_payload: string;
  nonce: string | null;
  scanned_at: string;
  sync_status: string;
  turnstile_opened: number;
  retry_count: number;
  last_error: string | null;
  synced_at: string | null;
}

/**
 * better-sqlite3 repository implementing local Store-and-Forward persistence in kiosk.db.
 */
export class SqliteQueueRepository implements IKioskQueueRepository {
  private readonly db: Database.Database;

  constructor(dbPath: string) {
    this.db = new Database(dbPath);
    this.db.pragma('journal_mode = WAL');
    this.db.pragma('synchronous = NORMAL');
    this.initSchema();
  }

  private initSchema(): void {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS local_attendance_logs (
        transaction_id TEXT PRIMARY KEY,
        employee_id TEXT NOT NULL,
        factory_id TEXT NOT NULL,
        gate_id TEXT NOT NULL,
        direction TEXT NOT NULL CHECK (direction IN ('IN', 'OUT')),
        qr_payload TEXT NOT NULL,
        nonce TEXT,
        scanned_at TEXT NOT NULL,
        sync_status TEXT NOT NULL CHECK (
          sync_status IN ('ONLINE_VERIFIED', 'OFFLINE_UNVERIFIED', 'SYNCED', 'SYNC_FAILED')
        ),
        turnstile_opened INTEGER NOT NULL DEFAULT 0,
        retry_count INTEGER NOT NULL DEFAULT 0,
        last_error TEXT,
        synced_at TEXT
      );

      CREATE INDEX IF NOT EXISTS idx_local_logs_sync_status
        ON local_attendance_logs (sync_status, scanned_at);

      CREATE INDEX IF NOT EXISTS idx_local_logs_nonce
        ON local_attendance_logs (nonce);
    `);
  }

  saveRecord(record: KioskAttendanceRecord): void {
    const stmt = this.db.prepare(`
      INSERT INTO local_attendance_logs (
        transaction_id, employee_id, factory_id, gate_id, direction,
        qr_payload, nonce, scanned_at, sync_status, turnstile_opened,
        retry_count, last_error, synced_at
      ) VALUES (
        @transactionId, @employeeId, @factoryId, @gateId, @direction,
        @qrPayload, @nonce, @scannedAt, @syncStatus, @turnstileOpened,
        @retryCount, @lastError, @syncedAt
      )
    `);

    stmt.run({
      ...record,
      turnstileOpened: record.turnstileOpened ? 1 : 0,
    });
  }

  hasRecentNonce(nonce: string): boolean {
    const row = this.db
      .prepare('SELECT 1 FROM local_attendance_logs WHERE nonce = ? LIMIT 1')
      .get(nonce);
    return Boolean(row);
  }

  findPendingSync(limit = 50): KioskAttendanceRecord[] {
    const rows = this.db
      .prepare(
        `SELECT * FROM local_attendance_logs
         WHERE sync_status = 'OFFLINE_UNVERIFIED'
         ORDER BY scanned_at ASC
         LIMIT ?`,
      )
      .all(limit) as SqliteRow[];
    return rows.map((row) => this.mapRow(row));
  }

  findRecent(limit = 50): KioskAttendanceRecord[] {
    const rows = this.db
      .prepare(
        `SELECT * FROM local_attendance_logs
         ORDER BY scanned_at DESC
         LIMIT ?`,
      )
      .all(limit) as SqliteRow[];
    return rows.map((row) => this.mapRow(row));
  }

  countByStatus(): Record<KioskSyncStatus, number> {
    const counts: Record<KioskSyncStatus, number> = {
      ONLINE_VERIFIED: 0,
      OFFLINE_UNVERIFIED: 0,
      SYNCED: 0,
      SYNC_FAILED: 0,
    };

    const rows = this.db
      .prepare(
        `SELECT sync_status, COUNT(*) AS total
         FROM local_attendance_logs
         GROUP BY sync_status`,
      )
      .all() as Array<{ sync_status: KioskSyncStatus; total: number }>;

    for (const row of rows) {
      if (row.sync_status in counts) {
        counts[row.sync_status] = row.total;
      }
    }
    return counts;
  }

  markSynced(transactionId: string, syncedAt: string): void {
    this.db
      .prepare(
        `UPDATE local_attendance_logs
         SET sync_status = 'SYNCED', synced_at = ?, last_error = NULL
         WHERE transaction_id = ?`,
      )
      .run(syncedAt, transactionId);
  }

  markSyncFailed(transactionId: string, errorMessage: string): void {
    this.db
      .prepare(
        `UPDATE local_attendance_logs
         SET sync_status = 'SYNC_FAILED', retry_count = retry_count + 1, last_error = ?
         WHERE transaction_id = ?`,
      )
      .run(errorMessage, transactionId);
  }

  incrementRetry(transactionId: string, errorMessage: string): void {
    this.db
      .prepare(
        `UPDATE local_attendance_logs
         SET retry_count = retry_count + 1, last_error = ?
         WHERE transaction_id = ?`,
      )
      .run(errorMessage, transactionId);
  }

  close(): void {
    this.db.close();
  }

  private mapRow(row: SqliteRow): KioskAttendanceRecord {
    return {
      transactionId: row.transaction_id,
      employeeId: row.employee_id,
      factoryId: row.factory_id,
      gateId: row.gate_id,
      direction: row.direction as GateDirection,
      qrPayload: row.qr_payload,
      nonce: row.nonce,
      scannedAt: row.scanned_at,
      syncStatus: row.sync_status as KioskSyncStatus,
      turnstileOpened: row.turnstile_opened === 1,
      retryCount: row.retry_count,
      lastError: row.last_error,
      syncedAt: row.synced_at,
    };
  }
}
