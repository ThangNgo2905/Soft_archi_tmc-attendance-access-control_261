package com.tmc.aac.features.kiosk.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Wire format: v1|employeeId|issuedAtEpochSecond|expiresAtEpochSecond|nonce|keyId|base64url(hmac). */
public record QrToken(String employeeId, long issuedAt, long expiresAt, String nonce, String keyId, String signature) {
    public static QrToken parse(String payload) {
        if (payload == null || payload.length() > 2048) throw new AccessRejected("MALFORMED_QR");
        String[] fields = payload.split("\\|", -1);
        if (fields.length != 7 || !fields[0].equals("v1")) throw new AccessRejected("MALFORMED_QR");
        for (int i : new int[]{1, 4, 5}) {
            if (!fields[i].matches("[A-Za-z0-9_-]{1,100}")) throw new AccessRejected("MALFORMED_QR");
        }
        try {
            return new QrToken(fields[1], Long.parseLong(fields[2]), Long.parseLong(fields[3]), fields[4], fields[5], fields[6]);
        } catch (NumberFormatException ex) { throw new AccessRejected("MALFORMED_QR"); }
    }

    public String unsigned() {
        return String.join("|", "v1", employeeId, Long.toString(issuedAt), Long.toString(expiresAt), nonce, keyId);
    }

    public String encode() { return unsigned() + "|" + signature; }

    public QrToken sign(String secret) {
        return new QrToken(employeeId, issuedAt, expiresAt, nonce, keyId,
                Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(unsigned(), secret)));
    }

    public boolean hasValidSignature(String secret) {
        try {
            return MessageDigest.isEqual(hmac(unsigned(), secret), Base64.getUrlDecoder().decode(signature));
        } catch (IllegalArgumentException ex) { return false; }
    }

    private static byte[] hmac(String text, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException ex) { throw new IllegalStateException("HMAC unavailable", ex); }
    }
}
