import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { SqliteQueueRepository } from '../adapters/sqlite-queue.repository';
import { ProcessScanUseCase } from '../application/process-scan.usecase';
import { SyncAgent } from '../application/sync-agent';
import {
  BackendCallOutcome,
  BackendCheckInPayload,
  IBackendGateway,
  ITurnstileGateway,
} from '../domain/ports';
import { computeQrHmacSignature, validateQrAtEdge } from '../domain/qr-parser';

class FakeBackendGateway implements IBackendGateway {
  public mode: 'ONLINE_ACCEPT' | 'ONLINE_REJECT' | 'OFFLINE' = 'ONLINE_ACCEPT';
  public receivedSyncPayloads: BackendCheckInPayload[] = [];
  private simulatedOffline = false;

  isSimulatedOffline(): boolean {
    return this.simulatedOffline;
  }

  setSimulatedOffline(offline: boolean): void {
    this.simulatedOffline = offline;
  }

  async checkHealth(): Promise<boolean> {
    return this.mode !== 'OFFLINE' && !this.simulatedOffline;
  }

  async checkIn(_payload: BackendCheckInPayload): Promise<BackendCallOutcome> {
    if (this.mode === 'OFFLINE' || this.simulatedOffline) {
      return { kind: 'UNREACHABLE', error: 'ECONNREFUSED' };
    }
    if (this.mode === 'ONLINE_REJECT') {
      return {
        kind: 'REJECTED',
        reasonCode: 'ANTI_PASSBACK_VIOLATION',
        message: 'Công nhân vừa quẹt IN trước đó.',
      };
    }
    return {
      kind: 'ACCEPTED',
      turnstileOpened: false,
      message: 'Check-in thành công.',
    };
  }

  async syncOfflineRecord(payload: BackendCheckInPayload): Promise<BackendCallOutcome> {
    this.receivedSyncPayloads.push(payload);
    return this.checkIn(payload);
  }
}

class FakeTurnstileGateway implements ITurnstileGateway {
  public unlockedCalls: Array<{ gateId: string; transactionId: string }> = [];

  async unlockGate(gateId: string, transactionId: string) {
    this.unlockedCalls.push({ gateId, transactionId });
    return { success: true, status: 'OPEN_CONFIRMED' };
  }
}

function createTempRepo(): { repo: SqliteQueueRepository; cleanup: () => void } {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'tmc-kiosk-test-'));
  const dbPath = path.join(dir, 'kiosk.db');
  const repo = new SqliteQueueRepository(dbPath);
  return {
    repo,
    cleanup: () => {
      repo.close();
      fs.rmSync(dir, { recursive: true, force: true });
    },
  };
}

test('validateQrAtEdge verifies HMAC-SHA256 signature and rejects expired QR', () => {
  const secret = 'tmc-shared-secret';
  const now = 1_700_000_000_000;
  const nonce = 'nonce-123';
  const sig = computeQrHmacSignature('EMP001', now, nonce, secret);

  const validQr = JSON.stringify({
    employeeId: 'EMP001',
    timestamp: now,
    nonce,
    signature: sig,
  });

  const ok = validateQrAtEdge(validQr, {
    hmacSecret: secret,
    ttlMs: 30_000,
    clockSkewMs: 5_000,
    nowMs: now + 10_000,
  });
  assert.equal(ok.valid, true);
  assert.equal(ok.parsed?.employeeId, 'EMP001');

  const expired = validateQrAtEdge(validQr, {
    hmacSecret: secret,
    ttlMs: 30_000,
    clockSkewMs: 5_000,
    nowMs: now + 40_000,
  });
  assert.equal(expired.valid, false);
  assert.equal(expired.reasonCode, 'QR_EXPIRED');

  const tamperedQr = JSON.stringify({
    employeeId: 'EMP999',
    timestamp: now,
    nonce,
    signature: sig,
  });
  const tampered = validateQrAtEdge(tamperedQr, {
    hmacSecret: secret,
    ttlMs: 30_000,
    clockSkewMs: 5_000,
    nowMs: now,
  });
  assert.equal(tampered.valid, false);
  assert.equal(tampered.reasonCode, 'QR_INVALID_SIGNATURE');
});

test('ProcessScanUseCase handles Online flow, Replay detection, and Offline Store-and-Forward + SyncAgent', async () => {
  const { repo, cleanup } = createTempRepo();
  try {
    const backend = new FakeBackendGateway();
    const turnstile = new FakeTurnstileGateway();
    const secret = 'tmc-secret';
    const useCase = new ProcessScanUseCase(repo, backend, turnstile, {
      edgeHmacSecret: secret,
      qrTtlMs: 30_000,
      qrClockSkewMs: 5_000,
    });
    const syncAgent = new SyncAgent(repo, backend, 5_000);

    // 1. Online normal scan
    const now1 = Date.now();
    const qr1 = JSON.stringify({
      employeeId: 'EMP001',
      timestamp: now1,
      nonce: 'n-online-1',
      signature: computeQrHmacSignature('EMP001', now1, 'n-online-1', secret),
    });
    const res1 = await useCase.execute({
      qrPayload: qr1,
      factoryId: 'Binh Duong',
      gateId: 'GATE_BD_IN_01',
      direction: 'IN',
    });
    assert.equal(res1.decision, 'GRANTED');
    assert.equal(res1.syncStatus, 'ONLINE_VERIFIED');
    assert.equal(res1.turnstileOpened, true);
    assert.equal(turnstile.unlockedCalls.length, 1);

    // 2. Replay attack with same nonce
    const replay = await useCase.execute({
      qrPayload: qr1,
      factoryId: 'Binh Duong',
      gateId: 'GATE_BD_IN_01',
      direction: 'IN',
    });
    assert.equal(replay.decision, 'DENIED');
    assert.equal(replay.reasonCode, 'REPLAY_NONCE_DETECTED');
    assert.equal(replay.turnstileOpened, false);

    // 3. Offline mode: saves OFFLINE_UNVERIFIED and still opens turnstile
    backend.mode = 'OFFLINE';
    const now2 = Date.now();
    const qr2 = JSON.stringify({
      employeeId: 'EMP002',
      timestamp: now2,
      nonce: 'n-offline-2',
      signature: computeQrHmacSignature('EMP002', now2, 'n-offline-2', secret),
    });
    const res2 = await useCase.execute({
      qrPayload: qr2,
      factoryId: 'Long An',
      gateId: 'GATE_LA_IN_01',
      direction: 'IN',
    });
    assert.equal(res2.decision, 'OFFLINE_STORED');
    assert.equal(res2.syncStatus, 'OFFLINE_UNVERIFIED');
    assert.equal(res2.turnstileOpened, true);
    assert.equal(turnstile.unlockedCalls.length, 2);
    assert.equal(repo.countByStatus().OFFLINE_UNVERIFIED, 1);

    // 4. Backend comes back online -> SyncAgent forwards record and marks SYNCED
    backend.mode = 'ONLINE_ACCEPT';
    const syncSummary = await syncAgent.runOnce();
    assert.equal(syncSummary.attempted, 1);
    assert.equal(syncSummary.synced, 1);
    assert.equal(syncSummary.failed, 0);
    assert.equal(repo.countByStatus().OFFLINE_UNVERIFIED, 0);
    assert.equal(repo.countByStatus().SYNCED, 1);
    assert.equal(backend.receivedSyncPayloads.length, 1);
    assert.equal(backend.receivedSyncPayloads[0].offlineSync, true);
  } finally {
    cleanup();
  }
});
