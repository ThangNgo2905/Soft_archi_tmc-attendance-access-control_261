package com.tmc.aac.features.kiosk.domain;

import java.time.Instant;

public final class QrVerifier {
    public void verify(QrToken token, EmployeeAccess employee, DeviceIdentity device, Instant now) {
        if (!employee.active()) throw new AccessRejected("EMPLOYEE_INACTIVE");
        if (!token.keyId().equals(employee.keyId()) || !token.hasValidSignature(employee.secret()))
            throw new AccessRejected("INVALID_SIGNATURE");
        long epoch = now.getEpochSecond();
        if (token.issuedAt() > epoch || token.expiresAt() <= epoch || token.expiresAt() <= token.issuedAt()
                || token.issuedAt() < epoch - 30 || token.expiresAt() > epoch + 30)
            throw new AccessRejected("QR_EXPIRED_OR_INVALID_TIME");
        if (token.expiresAt() - token.issuedAt() > 30) throw new AccessRejected("INVALID_TTL");
        if (!employee.factoryId().equals(device.factoryId())) throw new AccessRejected("WRONG_FACTORY");
    }
}
