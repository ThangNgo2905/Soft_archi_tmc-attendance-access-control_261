package com.tmc.aac.features.kiosk.repository;

/** A failure before connection differs from an ambiguous request with a lost response. */
public class RemoteUnavailable extends RuntimeException {
    private final boolean definitelyNotSent;
    public RemoteUnavailable(boolean definitelyNotSent) {
        super("Remote service unavailable");
        this.definitelyNotSent = definitelyNotSent;
    }
    public boolean definitelyNotSent() { return definitelyNotSent; }
}
