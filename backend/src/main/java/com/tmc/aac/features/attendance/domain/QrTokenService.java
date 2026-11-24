package com.tmc.aac.features.attendance.domain;

import com.tmc.aac.shared.exception.InvalidQrException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Pure domain service for parsing, generating, and verifying HMAC-SHA256 Dynamic QR tokens (FR-01).
 */
public class QrTokenService {

	private static final Pattern PLAIN_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{2,64}$");
	private static final ObjectMapper JSON_MAPPER = JsonMapper.builder().build();

	private final String hmacSecret;
	private final long ttlMillis;
	private final long clockSkewMillis;
	private final boolean allowUnsignedDev;

	public QrTokenService(
			String hmacSecret,
			long ttlSeconds,
			long clockSkewSeconds,
			boolean allowUnsignedDev) {
		this.hmacSecret = hmacSecret != null ? hmacSecret : "";
		this.ttlMillis = Math.max(ttlSeconds, 1) * 1000L;
		this.clockSkewMillis = Math.max(clockSkewSeconds, 0) * 1000L;
		this.allowUnsignedDev = allowUnsignedDev;
	}

	public ParsedQrToken parseAndValidate(String rawPayload, Instant referenceTime) {
		if (rawPayload == null || rawPayload.isBlank()) {
			throw new InvalidQrException("INVALID_QR_FORMAT: QR payload must not be blank");
		}

		String trimmed = rawPayload.trim();
		ParsedQrToken parsed = parseToken(trimmed);
		validateTimestamp(parsed, referenceTime);
		validateSignature(parsed);
		return parsed;
	}

	public String generateDynamicQrPayload(String employeeCode, Instant issuedAt, String nonce) {
		long timestamp = issuedAt.toEpochMilli();
		String signature = computeHmacHex(employeeCode, timestamp, nonce);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("employeeId", employeeCode);
		payload.put("timestamp", timestamp);
		payload.put("nonce", nonce);
		payload.put("signature", signature);
		return JSON_MAPPER.writeValueAsString(payload);
	}

	public String computeHmacHex(String employeeCode, long timestampMillis, String nonce) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			SecretKeySpec keySpec = new SecretKeySpec(
					hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
			mac.init(keySpec);
			String message = employeeCode + ":" + timestampMillis + ":" + nonce;
			byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(rawHmac);
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Failed to compute QR HMAC-SHA256", ex);
		}
	}

	private ParsedQrToken parseToken(String trimmed) {
		if (trimmed.startsWith("{")) {
			try {
				JsonNode node = JSON_MAPPER.readTree(trimmed);
				String employeeCode = firstText(node, "employeeCode", "employeeId", "empId", "employee_id");
				if (employeeCode == null || employeeCode.isBlank()) {
					throw new InvalidQrException("INVALID_QR_FORMAT: Missing employeeId/employeeCode in QR JSON");
				}
				Long timestamp = extractLong(node, "timestamp", "ts");
				String nonce = firstText(node, "nonce", "salt");
				String signature = firstText(node, "signature", "sig");
				return new ParsedQrToken(employeeCode.trim(), timestamp, trimToNull(nonce), trimToNull(signature));
			} catch (InvalidQrException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new InvalidQrException("INVALID_QR_FORMAT: Malformed QR JSON payload");
			}
		}

		if (allowUnsignedDev && PLAIN_CODE_PATTERN.matcher(trimmed).matches()) {
			return new ParsedQrToken(trimmed, null, null, null);
		}

		throw new InvalidQrException("INVALID_QR_FORMAT: Dynamic JSON QR payload is required");
	}

	private void validateTimestamp(ParsedQrToken parsed, Instant referenceTime) {
		if (parsed.timestampMillis() == null) {
			if (!allowUnsignedDev) {
				throw new InvalidQrException("QR_MISSING_TIMESTAMP: Dynamic QR requires timestamp");
			}
			return;
		}

		long nowMillis = referenceTime.toEpochMilli();
		long ageMillis = nowMillis - parsed.timestampMillis();
		if (ageMillis > ttlMillis + clockSkewMillis) {
			throw new InvalidQrException("QR_EXPIRED: Dynamic QR token has expired");
		}
		if (parsed.timestampMillis() - nowMillis > clockSkewMillis) {
			throw new InvalidQrException("QR_INVALID_TIMESTAMP: QR timestamp is in the future");
		}
	}

	private void validateSignature(ParsedQrToken parsed) {
		if (allowUnsignedDev && parsed.signature() == null) {
			return;
		}

		if (parsed.timestampMillis() == null || parsed.nonce() == null || parsed.signature() == null) {
			throw new InvalidQrException("QR_MISSING_SIGNATURE: Dynamic QR requires timestamp, nonce, and signature");
		}

		String expected = computeHmacHex(parsed.employeeCode(), parsed.timestampMillis(), parsed.nonce());
		byte[] expectedBytes = expected.toLowerCase().getBytes(StandardCharsets.UTF_8);
		byte[] actualBytes = parsed.signature().toLowerCase().getBytes(StandardCharsets.UTF_8);

		if (!MessageDigest.isEqual(expectedBytes, actualBytes)) {
			throw new InvalidQrException("QR_INVALID_SIGNATURE: Invalid Dynamic QR HMAC signature");
		}
	}

	private static String firstText(JsonNode node, String... keys) {
		for (String key : keys) {
			JsonNode child = node.get(key);
			if (child != null && child.isString()) {
				return child.asString();
			}
		}
		return null;
	}

	private static Long extractLong(JsonNode node, String... keys) {
		for (String key : keys) {
			JsonNode child = node.get(key);
			if (child == null || child.isNull()) {
				continue;
			}
			if (child.isIntegralNumber()) {
				return child.asLong();
			}
			if (child.isString() && child.asString().matches("^\\d+$")) {
				return Long.parseLong(child.asString());
			}
		}
		return null;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
