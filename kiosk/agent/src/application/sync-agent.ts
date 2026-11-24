import { IBackendGateway, IKioskQueueRepository } from '../domain/ports';

export interface SyncRunSummary {
  attempted: number;
  synced: number;
  failed: number;
  stillOffline: number;
}

/**
 * Background agent that forwards OFFLINE_UNVERIFIED records from SQLite
 * to the central Backend once connectivity is restored.
 */
export class SyncAgent {
  private timer: NodeJS.Timeout | null = null;
  private running = false;

  constructor(
    private readonly queueRepo: IKioskQueueRepository,
    private readonly backendGateway: IBackendGateway,
    private readonly intervalMs: number,
  ) {}

  start(): void {
    if (this.timer) return;
    this.timer = setInterval(() => {
      void this.runOnce();
    }, this.intervalMs);
    this.timer.unref();
  }

  stop(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  async runOnce(): Promise<SyncRunSummary> {
    if (this.running) {
      return { attempted: 0, synced: 0, failed: 0, stillOffline: 0 };
    }

    this.running = true;
    const summary: SyncRunSummary = {
      attempted: 0,
      synced: 0,
      failed: 0,
      stillOffline: 0,
    };

    try {
      const pending = this.queueRepo.findPendingSync(50);
      if (pending.length === 0) {
        return summary;
      }

      for (const record of pending) {
        summary.attempted += 1;
        const outcome = await this.backendGateway.syncOfflineRecord({
          transactionId: record.transactionId,
          employeeId: record.employeeId,
          factoryId: record.factoryId,
          gateId: record.gateId,
          direction: record.direction,
          qrPayload: record.qrPayload,
          scannedAt: record.scannedAt,
          offlineSync: true,
        });

        if (outcome.kind === 'ACCEPTED') {
          this.queueRepo.markSynced(record.transactionId, new Date().toISOString());
          summary.synced += 1;
        } else if (outcome.kind === 'REJECTED') {
          this.queueRepo.markSyncFailed(
            record.transactionId,
            `${outcome.reasonCode}: ${outcome.message}`,
          );
          summary.failed += 1;
        } else {
          this.queueRepo.incrementRetry(record.transactionId, outcome.error);
          summary.stillOffline += 1;
          // Stop batch early when Backend is still unreachable
          break;
        }
      }

      return summary;
    } finally {
      this.running = false;
    }
  }
}
