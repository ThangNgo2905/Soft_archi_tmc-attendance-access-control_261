import cors from 'cors';
import express from 'express';
import { HttpBackendGateway } from './adapters/http-backend.gateway';
import { HttpTurnstileGateway } from './adapters/http-turnstile.gateway';
import { SqliteQueueRepository } from './adapters/sqlite-queue.repository';
import { ProcessScanUseCase } from './application/process-scan.usecase';
import { SyncAgent } from './application/sync-agent';
import { loadConfig } from './config';
import { GateDirection } from './domain/kiosk-record';

const config = loadConfig();

const queueRepo = new SqliteQueueRepository(config.sqliteDbPath);
const backendGateway = new HttpBackendGateway({
  baseUrl: config.backendUrl,
  checkInPath: config.backendCheckInPath,
  syncPath: config.backendSyncPath,
  healthPath: config.backendHealthPath,
  timeoutMs: config.backendTimeoutMs,
});
const turnstileGateway = new HttpTurnstileGateway(
  config.turnstileUrl,
  config.backendTimeoutMs,
);

const processScanUseCase = new ProcessScanUseCase(
  queueRepo,
  backendGateway,
  turnstileGateway,
  {
    edgeHmacSecret: config.edgeHmacSecret,
    qrTtlMs: config.qrTtlMs,
    qrClockSkewMs: config.qrClockSkewMs,
  },
);

const syncAgent = new SyncAgent(queueRepo, backendGateway, config.syncIntervalMs);
syncAgent.start();

const app = express();
app.use(cors());
app.use(express.json());

app.get('/local/status', async (_req, res) => {
  const backendReachable = await backendGateway.checkHealth();
  res.json({
    ok: true,
    backendUrl: config.backendUrl,
    backendReachable,
    simulatedOffline: backendGateway.isSimulatedOffline(),
    turnstileUrl: config.turnstileUrl,
    sqliteDbPath: config.sqliteDbPath,
    defaultGate: {
      factoryId: config.defaultFactoryId,
      gateId: config.defaultGateId,
      direction: config.defaultDirection,
    },
    counts: queueRepo.countByStatus(),
  });
});

app.post('/local/scan', async (req, res) => {
  const { qrPayload, factoryId, gateId, direction } = req.body as {
    qrPayload?: string;
    factoryId?: string;
    gateId?: string;
    direction?: string;
  };

  if (typeof qrPayload !== 'string' || !qrPayload.trim()) {
    return res.status(400).json({ error: 'qrPayload is required' });
  }

  const normalizedDirection: GateDirection =
    direction?.toUpperCase() === 'OUT' ? 'OUT' : config.defaultDirection;

  const result = await processScanUseCase.execute({
    qrPayload,
    factoryId: factoryId?.trim() || config.defaultFactoryId,
    gateId: gateId?.trim() || config.defaultGateId,
    direction: direction ? (direction.toUpperCase() === 'OUT' ? 'OUT' : 'IN') : normalizedDirection,
  });

  const httpStatus = result.decision === 'DENIED' ? 422 : 200;
  return res.status(httpStatus).json(result);
});

app.get('/local/queue', (req, res) => {
  const limitRaw = Number(req.query.limit);
  const limit = Number.isFinite(limitRaw) && limitRaw > 0 ? Math.min(limitRaw, 200) : 50;
  res.json({
    counts: queueRepo.countByStatus(),
    records: queueRepo.findRecent(limit),
  });
});

app.post('/local/sync', async (_req, res) => {
  const summary = await syncAgent.runOnce();
  res.json({
    summary,
    counts: queueRepo.countByStatus(),
  });
});

app.post('/local/simulate-offline', (req, res) => {
  const offline = Boolean(req.body?.offline);
  backendGateway.setSimulatedOffline(offline);
  res.json({
    simulatedOffline: backendGateway.isSimulatedOffline(),
  });
});

const server = app.listen(config.port, () => {
  console.log(`[KIOSK AGENT] Listening on http://localhost:${config.port}`);
  console.log(`[KIOSK AGENT] SQLite DB: ${config.sqliteDbPath}`);
  console.log(`[KIOSK AGENT] Backend URL: ${config.backendUrl}`);
  console.log(`[KIOSK AGENT] Turnstile URL: ${config.turnstileUrl}`);
});

function shutdown(): void {
  syncAgent.stop();
  server.close(() => {
    queueRepo.close();
    process.exit(0);
  });
}

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
