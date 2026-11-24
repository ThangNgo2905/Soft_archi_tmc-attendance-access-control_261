import path from 'path';
import dotenv from 'dotenv';
import { GateDirection } from './domain/kiosk-record';

dotenv.config();

export interface KioskAgentConfig {
  port: number;
  defaultFactoryId: string;
  defaultGateId: string;
  defaultDirection: GateDirection;
  backendUrl: string;
  backendCheckInPath: string;
  backendSyncPath: string;
  backendHealthPath: string;
  backendTimeoutMs: number;
  turnstileUrl: string;
  sqliteDbPath: string;
  syncIntervalMs: number;
  edgeHmacSecret: string;
  qrTtlMs: number;
  qrClockSkewMs: number;
}

function parseNumber(raw: string | undefined, fallback: number): number {
  if (!raw) return fallback;
  const n = Number(raw);
  return Number.isFinite(n) && n > 0 ? n : fallback;
}

export function loadConfig(): KioskAgentConfig {
  const dirRaw = (process.env.DEFAULT_DIRECTION || 'IN').toUpperCase();
  const defaultDirection: GateDirection = dirRaw === 'OUT' ? 'OUT' : 'IN';

  return {
    port: parseNumber(process.env.KIOSK_AGENT_PORT, 3001),
    defaultFactoryId: process.env.DEFAULT_FACTORY_ID || 'Binh Duong',
    defaultGateId: process.env.DEFAULT_GATE_ID || 'GATE_BD_IN_01',
    defaultDirection,
    backendUrl: (process.env.BACKEND_URL || 'http://localhost:3000').replace(/\/+$/, ''),
    backendCheckInPath: process.env.BACKEND_CHECKIN_PATH || '/api/attendance/check-in',
    backendSyncPath: process.env.BACKEND_SYNC_PATH || '/api/attendance/sync',
    backendHealthPath: process.env.BACKEND_HEALTH_PATH || '/health',
    backendTimeoutMs: parseNumber(process.env.BACKEND_TIMEOUT_MS, 1500),
    turnstileUrl: process.env.TURNSTILE_URL || 'http://localhost:4000/api/turnstile/unlock',
    sqliteDbPath: path.resolve(process.cwd(), process.env.SQLITE_DB_PATH || './kiosk.db'),
    syncIntervalMs: parseNumber(process.env.SYNC_INTERVAL_MS, 5000),
    edgeHmacSecret: process.env.EDGE_HMAC_SECRET || '',
    qrTtlMs: parseNumber(process.env.QR_TTL_MS, 30000),
    qrClockSkewMs: parseNumber(process.env.QR_CLOCK_SKEW_MS, 5000),
  };
}
