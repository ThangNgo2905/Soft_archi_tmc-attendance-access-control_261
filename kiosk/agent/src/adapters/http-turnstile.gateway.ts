import { ITurnstileGateway } from '../domain/ports';

/**
 * HTTP adapter calling mock-servers/server.js (POST /api/turnstile/unlock).
 */
export class HttpTurnstileGateway implements ITurnstileGateway {
  constructor(
    private readonly unlockUrl: string,
    private readonly timeoutMs = 1500,
  ) {}

  async unlockGate(
    gateId: string,
    transactionId: string,
  ): Promise<{ success: boolean; status: string; error?: string }> {
    try {
      const res = await fetch(this.unlockUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ gateId, transactionId }),
        signal: AbortSignal.timeout(this.timeoutMs),
      });

      if (!res.ok) {
        return {
          success: false,
          status: `HTTP_${res.status}`,
          error: `Turnstile API returned HTTP ${res.status}`,
        };
      }

      const data = (await res.json().catch(() => ({}))) as {
        success?: boolean;
        status?: string;
      };

      return {
        success: data.success !== false,
        status: data.status || 'OPEN_CONFIRMED',
      };
    } catch (err) {
      return {
        success: false,
        status: 'UNREACHABLE',
        error: err instanceof Error ? err.message : 'Turnstile unreachable',
      };
    }
  }
}
