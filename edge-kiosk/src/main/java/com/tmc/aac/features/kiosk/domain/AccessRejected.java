package com.tmc.aac.features.kiosk.domain;

public class AccessRejected extends RuntimeException {
    public AccessRejected(String reason) { super(reason); }
}
