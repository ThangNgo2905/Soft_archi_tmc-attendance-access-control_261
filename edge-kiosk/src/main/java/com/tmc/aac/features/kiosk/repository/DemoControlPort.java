package com.tmc.aac.features.kiosk.repository;

public interface DemoControlPort {
    record MockState(String service, String mode, int openCount, int eventCount, int authorizationCount) {}
    MockState state(String service);
    MockState mode(String service, String mode);
    void resolve(String transactionId, String outcome);
}
