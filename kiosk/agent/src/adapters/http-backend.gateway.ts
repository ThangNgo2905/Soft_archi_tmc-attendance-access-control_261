import {
  BackendCallOutcome,
  BackendCheckInPayload,
  IBackendGateway,
} from '../domain/ports';

export interface HttpBackendGatewayOptions {
  baseUrl: string;
  checkInPath: string;
  syncPath: string;
  healthPath: string;
  timeoutMs: number;
}

/**
 * HTTP adapter communicating with the central TMC Modular Monolith Backend.
 * Includes a simulatedOffline switch for deterministic network-drop demos.
 */
export class HttpBackendGateway implements IBackendGateway {
  private simulatedOffline = false;

  constructor(private readonly options: HttpBackendGatewayOptions) {}

  isSimulatedOffline(): boolean {
    return this.simulatedOffline;
  }

  setSimulatedOffline(offline: boolean): void {
    this.simulatedOffline = offline;
  }

  checkIn(payload: BackendCheckInPayload): Promise<BackendCallOutcome> {
    return this.postToBackend(this.options.checkInPath, payload);
  }

  syncOfflineRecord(payload: BackendCheckInPayload): Promise<BackendCallOutcome> {
    return this.postToBackend(this.options.syncPath, {
      ...payload,
      offlineSync: true,
    });
  }

  async checkHealth(): Promise<boolean> {
    if (this.simulatedOffline) {
      return false;
    }

    try {
      const res = await fetch(`${this.options.baseUrl}${this.options.healthPath}`, {
        method: 'GET',
        signal: AbortSignal.timeout(this.options.timeoutMs),
      });
      return res.ok;
    } catch {
      return false;
    }
  }

  private async postToBackend(
    endpointPath: string,
    payload: BackendCheckInPayload,
  ): Promise<BackendCallOutcome> {
    if (this.simulatedOffline) {
      return {
        kind: 'UNREACHABLE',
        error: 'SIMULATED_OFFLINE',
      };
    }

    try {
      const res = await fetch(`${this.options.baseUrl}${endpointPath}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        signal: AbortSignal.timeout(this.options.timeoutMs),
      });

      const body = (await res.json().catch(() => ({}))) as Record<string, unknown>;

      if (res.ok) {
        return {
          kind: 'ACCEPTED',
          turnstileOpened: body.turnstileOpened === true,
          message:
            typeof body.message === 'string'
              ? body.message
              : 'Xác thực thành công qua Backend trung tâm.',
        };
      }

      if (res.status >= 400 && res.status < 500 && res.status !== 408 && res.status !== 429) {
        return {
          kind: 'REJECTED',
          reasonCode:
            typeof body.reasonCode === 'string'
              ? body.reasonCode
              : `HTTP_${res.status}`,
          message:
            typeof body.message === 'string'
              ? body.message
              : 'Backend từ chối mã QR.',
        };
      }

      return {
        kind: 'UNREACHABLE',
        error: `Backend HTTP ${res.status}`,
      };
    } catch (err) {
      return {
        kind: 'UNREACHABLE',
        error: err instanceof Error ? err.message : 'Network error',
      };
    }
  }
}
