import crypto from 'crypto';
import {
  KioskAttendanceRecord,
  ProcessScanInput,
  ProcessScanResult,
} from '../domain/kiosk-record';
import {
  IBackendGateway,
  IKioskQueueRepository,
  ITurnstileGateway,
} from '../domain/ports';
import { parseQrPayload, validateQrAtEdge } from '../domain/qr-parser';

export interface ProcessScanOptions {
  edgeHmacSecret: string;
  qrTtlMs: number;
  qrClockSkewMs: number;
}

/**
 * Use Case: Processes a QR scan at KioskEdge.
 * 1) Validates QR structure and local replay cache.
 * 2) Attempts real-time verification with central Backend.
 * 3) On Backend network failure, falls back to Edge validation + SQLite Store-and-Forward
 *    (OFFLINE_UNVERIFIED) and triggers local Turnstile unlock so factory gate stays operational.
 */
export class ProcessScanUseCase {
  constructor(
    private readonly queueRepo: IKioskQueueRepository,
    private readonly backendGateway: IBackendGateway,
    private readonly turnstileGateway: ITurnstileGateway,
    private readonly options: ProcessScanOptions,
  ) {}

  async execute(input: ProcessScanInput): Promise<ProcessScanResult> {
    const startMs = Date.now();
    const scannedAt = new Date(startMs).toISOString();
    const transactionId = crypto.randomUUID();

    const parsed = parseQrPayload(input.qrPayload);
    if (!parsed) {
      return {
        decision: 'DENIED',
        transactionId,
        employeeId: null,
        gateId: input.gateId,
        direction: input.direction,
        syncStatus: null,
        turnstileOpened: false,
        reasonCode: 'INVALID_QR_FORMAT',
        message: 'Mã QR không hợp lệ hoặc không đọc được thông tin nhân viên.',
        latencyMs: Date.now() - startMs,
        scannedAt,
      };
    }

    if (parsed.nonce && this.queueRepo.hasRecentNonce(parsed.nonce)) {
      return {
        decision: 'DENIED',
        transactionId,
        employeeId: parsed.employeeId,
        gateId: input.gateId,
        direction: input.direction,
        syncStatus: null,
        turnstileOpened: false,
        reasonCode: 'REPLAY_NONCE_DETECTED',
        message: 'Phát hiện mã QR đã được sử dụng (chống phát lại / Replay Attack).',
        latencyMs: Date.now() - startMs,
        scannedAt,
      };
    }

    const backendOutcome = await this.backendGateway.checkIn({
      transactionId,
      employeeId: parsed.employeeId,
      factoryId: input.factoryId,
      gateId: input.gateId,
      direction: input.direction,
      qrPayload: parsed.raw,
      scannedAt,
    });

    if (backendOutcome.kind === 'REJECTED') {
      return {
        decision: 'DENIED',
        transactionId,
        employeeId: parsed.employeeId,
        gateId: input.gateId,
        direction: input.direction,
        syncStatus: null,
        turnstileOpened: false,
        reasonCode: backendOutcome.reasonCode,
        message: backendOutcome.message,
        latencyMs: Date.now() - startMs,
        scannedAt,
      };
    }

    if (backendOutcome.kind === 'ACCEPTED') {
      const turnstileOpened = backendOutcome.turnstileOpened
        ? true
        : (await this.turnstileGateway.unlockGate(input.gateId, transactionId)).success;

      const record: KioskAttendanceRecord = {
        transactionId,
        employeeId: parsed.employeeId,
        factoryId: input.factoryId,
        gateId: input.gateId,
        direction: input.direction,
        qrPayload: parsed.raw,
        nonce: parsed.nonce,
        scannedAt,
        syncStatus: 'ONLINE_VERIFIED',
        turnstileOpened,
        retryCount: 0,
        lastError: null,
        syncedAt: scannedAt,
      };
      this.queueRepo.saveRecord(record);

      return {
        decision: 'GRANTED',
        transactionId,
        employeeId: parsed.employeeId,
        gateId: input.gateId,
        direction: input.direction,
        syncStatus: 'ONLINE_VERIFIED',
        turnstileOpened,
        reasonCode: 'ONLINE_VERIFIED',
        message: backendOutcome.message,
        latencyMs: Date.now() - startMs,
        scannedAt,
      };
    }

    // Backend unreachable -> Edge Store-and-Forward mode
    const edgeCheck = validateQrAtEdge(parsed.raw, {
      hmacSecret: this.options.edgeHmacSecret || undefined,
      ttlMs: this.options.qrTtlMs,
      clockSkewMs: this.options.qrClockSkewMs,
      nowMs: startMs,
    });

    if (!edgeCheck.valid) {
      return {
        decision: 'DENIED',
        transactionId,
        employeeId: parsed.employeeId,
        gateId: input.gateId,
        direction: input.direction,
        syncStatus: null,
        turnstileOpened: false,
        reasonCode: edgeCheck.reasonCode,
        message: `[Offline Edge] ${edgeCheck.message}`,
        latencyMs: Date.now() - startMs,
        scannedAt,
      };
    }

    const turnstileResult = await this.turnstileGateway.unlockGate(
      input.gateId,
      transactionId,
    );

    const offlineRecord: KioskAttendanceRecord = {
      transactionId,
      employeeId: parsed.employeeId,
      factoryId: input.factoryId,
      gateId: input.gateId,
      direction: input.direction,
      qrPayload: parsed.raw,
      nonce: parsed.nonce,
      scannedAt,
      syncStatus: 'OFFLINE_UNVERIFIED',
      turnstileOpened: turnstileResult.success,
      retryCount: 0,
      lastError: backendOutcome.error,
      syncedAt: null,
    };
    this.queueRepo.saveRecord(offlineRecord);

    return {
      decision: 'OFFLINE_STORED',
      transactionId,
      employeeId: parsed.employeeId,
      gateId: input.gateId,
      direction: input.direction,
      syncStatus: 'OFFLINE_UNVERIFIED',
      turnstileOpened: turnstileResult.success,
      reasonCode: 'OFFLINE_UNVERIFIED',
      message:
        'Mất kết nối Backend: Đã lưu tạm vào SQLite (OFFLINE_UNVERIFIED) và kích hoạt mở cổng dự phòng.',
      latencyMs: Date.now() - startMs,
      scannedAt,
    };
  }
}
