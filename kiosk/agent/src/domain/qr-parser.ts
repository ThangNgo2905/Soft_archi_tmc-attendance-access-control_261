import crypto from 'crypto';
import { ParsedQrPayload } from './kiosk-record';

export interface QrValidationOptions {
  hmacSecret?: string;
  ttlMs: number;
  clockSkewMs: number;
  nowMs?: number;
}

export interface QrValidationResult {
  valid: boolean;
  reasonCode: string;
  message: string;
  parsed?: ParsedQrPayload;
}

/**
 * Parses QR string scanned from webcam into normalized fields.
 * Supports:
 *  1) Canonical JSON Dynamic QR: {"employeeId":"EMP001","timestamp":173...,"nonce":"...","signature":"..."}
 *  2) Legacy delimited text: "EMP001|173..." or "EMP001:173..."
 *  3) Plain employee code text (for early prototype testing)
 */
export function parseQrPayload(rawInput: string): ParsedQrPayload | null {
  const trimmed = rawInput.trim();
  if (!trimmed) {
    return null;
  }

  if (trimmed.startsWith('{')) {
    try {
      const obj = JSON.parse(trimmed) as Record<string, unknown>;
      const employeeIdRaw = obj.employeeId ?? obj.empId ?? obj.employee_id;
      if (typeof employeeIdRaw !== 'string' || !employeeIdRaw.trim()) {
        return null;
      }
      const tsRaw = obj.timestamp ?? obj.ts;
      const timestamp =
        typeof tsRaw === 'number' && Number.isFinite(tsRaw)
          ? tsRaw
          : typeof tsRaw === 'string' && /^\d+$/.test(tsRaw)
            ? Number(tsRaw)
            : null;

      return {
        raw: trimmed,
        employeeId: employeeIdRaw.trim(),
        timestamp,
        nonce: typeof obj.nonce === 'string' && obj.nonce.trim() ? obj.nonce.trim() : null,
        signature:
          typeof obj.signature === 'string' && obj.signature.trim()
            ? obj.signature.trim()
            : typeof obj.sig === 'string' && obj.sig.trim()
              ? obj.sig.trim()
              : null,
        format: 'JSON_DYNAMIC',
      };
    } catch {
      return null;
    }
  }

  const delimiterMatch = /^([A-Za-z0-9_-]+)[|:](.+)$/.exec(trimmed);
  if (delimiterMatch) {
    const [, employeeId, rest] = delimiterMatch;
    const parts = rest.split(/[|:]/);
    const maybeTs = /^\d+$/.test(parts[0]) ? Number(parts[0]) : null;
    return {
      raw: trimmed,
      employeeId,
      timestamp: maybeTs,
      nonce: parts[1] || null,
      signature: parts[2] || null,
      format: 'DELIMITED_LEGACY',
    };
  }

  if (/^[A-Za-z0-9_.-]{2,64}$/.test(trimmed)) {
    return {
      raw: trimmed,
      employeeId: trimmed,
      timestamp: null,
      nonce: null,
      signature: null,
      format: 'PLAIN_TEXT',
    };
  }

  return null;
}

/**
 * Computes canonical HMAC-SHA256 signature matching the Mobile Web QR contract:
 * message = `${employeeId}:${timestamp}:${nonce}`
 */
export function computeQrHmacSignature(
  employeeId: string,
  timestamp: number,
  nonce: string,
  secret: string,
): string {
  return crypto
    .createHmac('sha256', secret)
    .update(`${employeeId}:${timestamp}:${nonce}`)
    .digest('hex');
}

/**
 * Performs Edge-level QR validation when operating in Offline mode.
 * If hmacSecret is configured, enforces dynamic QR TTL + HMAC-SHA256 verification.
 */
export function validateQrAtEdge(
  rawInput: string,
  options: QrValidationOptions,
): QrValidationResult {
  const parsed = parseQrPayload(rawInput);
  if (!parsed) {
    return {
      valid: false,
      reasonCode: 'INVALID_QR_FORMAT',
      message: 'Mã QR không đúng định dạng hệ thống TMC.',
    };
  }

  const nowMs = options.nowMs ?? Date.now();

  if (parsed.timestamp !== null) {
    const ageMs = nowMs - parsed.timestamp;
    if (ageMs > options.ttlMs + options.clockSkewMs) {
      return {
        valid: false,
        reasonCode: 'QR_EXPIRED',
        message: 'Mã QR đã hết hạn (quá 30 giây). Vui lòng lấy mã mới trên điện thoại.',
        parsed,
      };
    }
    if (parsed.timestamp - nowMs > options.clockSkewMs) {
      return {
        valid: false,
        reasonCode: 'QR_INVALID_TIMESTAMP',
        message: 'Timestamp trên mã QR không hợp lệ (lệch đồng hồ).',
        parsed,
      };
    }
  }

  if (options.hmacSecret) {
    if (parsed.timestamp === null || !parsed.nonce || !parsed.signature) {
      return {
        valid: false,
        reasonCode: 'QR_MISSING_SIGNATURE',
        message: 'Chế độ Edge bảo mật yêu cầu Dynamic QR có timestamp, nonce và signature.',
        parsed,
      };
    }

    const expectedSig = computeQrHmacSignature(
      parsed.employeeId,
      parsed.timestamp,
      parsed.nonce,
      options.hmacSecret,
    );

    const actualBuf = Buffer.from(parsed.signature.toLowerCase(), 'utf8');
    const expectedBuf = Buffer.from(expectedSig.toLowerCase(), 'utf8');
    const sigMatches =
      actualBuf.length === expectedBuf.length &&
      crypto.timingSafeEqual(actualBuf, expectedBuf);

    if (!sigMatches) {
      return {
        valid: false,
        reasonCode: 'QR_INVALID_SIGNATURE',
        message: 'Chữ ký HMAC trên mã QR không hợp lệ.',
        parsed,
      };
    }
  }

  return {
    valid: true,
    reasonCode: 'EDGE_VALID',
    message: 'Hợp lệ tại Edge Kiosk.',
    parsed,
  };
}
