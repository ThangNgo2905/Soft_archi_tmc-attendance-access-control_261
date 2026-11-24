import {
  AgentStatusResponse,
  GateConfig,
  ProcessScanResult,
  QueueResponse,
  SyncTriggerResponse,
} from '../types';

const AGENT_BASE_URL = (
  import.meta.env.VITE_KIOSK_AGENT_URL || 'http://localhost:3001'
).replace(/\/+$/, '');

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${AGENT_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers || {}),
    },
  });

  const data = (await res.json()) as T;
  if (!res.ok && res.status !== 422) {
    throw new Error(`Kiosk Agent error (${res.status})`);
  }
  return data;
}

export const kioskAgentClient = {
  baseUrl: AGENT_BASE_URL,

  getStatus(): Promise<AgentStatusResponse> {
    return requestJson<AgentStatusResponse>('/local/status');
  },

  submitScan(qrPayload: string, gate: GateConfig): Promise<ProcessScanResult> {
    return requestJson<ProcessScanResult>('/local/scan', {
      method: 'POST',
      body: JSON.stringify({
        qrPayload,
        factoryId: gate.factoryId,
        gateId: gate.gateId,
        direction: gate.direction,
      }),
    });
  },

  getQueue(limit = 30): Promise<QueueResponse> {
    return requestJson<QueueResponse>(`/local/queue?limit=${limit}`);
  },

  triggerSync(): Promise<SyncTriggerResponse> {
    return requestJson<SyncTriggerResponse>('/local/sync', {
      method: 'POST',
    });
  },

  setSimulatedOffline(offline: boolean): Promise<{ simulatedOffline: boolean }> {
    return requestJson<{ simulatedOffline: boolean }>('/local/simulate-offline', {
      method: 'POST',
      body: JSON.stringify({ offline }),
    });
  },
};
