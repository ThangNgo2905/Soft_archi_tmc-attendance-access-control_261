package com.tmc.aac.features.attendance.domain;

import com.tmc.aac.shared.exception.InvalidQrException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrTokenServiceTest {

	private static final String SECRET = "test-hmac-secret";
	private final QrTokenService service = new QrTokenService(SECRET, 30, 5, false);

	@Test
	void generatesAndVerifiesValidDynamicQr() {
		Instant issuedAt = Instant.parse("2026-11-24T08:00:00Z");
		String payload = service.generateDynamicQrPayload("EMP001", issuedAt, "nonce-abc");

		ParsedQrToken parsed = service.parseAndValidate(payload, issuedAt.plusSeconds(15));
		assertEquals("EMP001", parsed.employeeCode());
		assertEquals("nonce-abc", parsed.nonce());
		assertEquals(issuedAt.toEpochMilli(), parsed.timestampMillis());
	}

	@Test
	void rejectsExpiredQrBeyondTtlAndClockSkew() {
		Instant issuedAt = Instant.parse("2026-11-24T08:00:00Z");
		String payload = service.generateDynamicQrPayload("EMP001", issuedAt, "nonce-exp");

		InvalidQrException ex = assertThrows(
				InvalidQrException.class,
				() -> service.parseAndValidate(payload, issuedAt.plusSeconds(36))
		);
		assertTrue(ex.getMessage().contains("QR_EXPIRED"));
	}

	@Test
	void rejectsTamperedSignature() {
		Instant issuedAt = Instant.parse("2026-11-24T08:00:00Z");
		String payload = service.generateDynamicQrPayload("EMP001", issuedAt, "nonce-1")
				.replace("EMP001", "EMP002");

		InvalidQrException ex = assertThrows(
				InvalidQrException.class,
				() -> service.parseAndValidate(payload, issuedAt.plusSeconds(2))
		);
		assertTrue(ex.getMessage().contains("QR_INVALID_SIGNATURE"));
	}

	@Test
	void rejectsStaticPlainTextQrWhenStrictModeEnabled() {
		InvalidQrException ex = assertThrows(
				InvalidQrException.class,
				() -> service.parseAndValidate("EMP001", Instant.now())
		);
		assertTrue(ex.getMessage().contains("INVALID_QR_FORMAT"));
	}
}
