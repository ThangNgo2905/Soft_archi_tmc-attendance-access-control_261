package com.tmc.aac.features.kiosk;

import com.tmc.aac.features.kiosk.domain.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PolicyTest {
    final Instant now = Instant.parse("2026-11-27T05:00:00Z");
    final EmployeeAccess employee = new EmployeeAccess("E1", "BD", "K1", "test-secret", true, LocalTime.MIN, LocalTime.MAX);
    final DeviceIdentity device = new DeviceIdentity("K1", "BD", "G1", Direction.IN, ZoneOffset.UTC);
    final QrVerifier verifier = new QrVerifier();
    QrToken token(long issued, long expires) { return new QrToken("E1", issued, expires, "nonce", "K1", "").sign(employee.secret()); }
    @Test void timeWindowIsStrictAtExpiryAndRejectsFutureOrExcessiveTtl() {
        long epoch = now.getEpochSecond();
        assertThatCode(() -> verifier.verify(token(epoch, epoch + 30), employee, device, now)).doesNotThrowAnyException();
        for (QrToken invalid : new QrToken[]{token(epoch - 30, epoch), token(epoch + 1, epoch + 30), token(epoch, epoch + 31), token(epoch, epoch)})
            assertThatThrownBy(() -> verifier.verify(invalid, employee, device, now)).isInstanceOf(AccessRejected.class);
    }
    @Test void overnightShiftHandlesBothSidesOfMidnightAndEndIsExclusive() {
        var overnight = new EmployeeAccess("E1", "BD", "K1", "secret", true, LocalTime.of(22, 0), LocalTime.of(6, 0));
        var policy = new LocalShiftPolicy();
        assertThatCode(() -> policy.verify(overnight, Instant.parse("2026-11-27T23:00:00Z"), ZoneOffset.UTC)).doesNotThrowAnyException();
        assertThatCode(() -> policy.verify(overnight, Instant.parse("2026-11-27T03:00:00Z"), ZoneOffset.UTC)).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.verify(overnight, Instant.parse("2026-11-27T06:00:00Z"), ZoneOffset.UTC)).isInstanceOf(AccessRejected.class);
    }
}
