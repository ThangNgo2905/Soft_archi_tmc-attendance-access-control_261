package com.tmc.aac.shared.exception;

public class ScanConflict extends RuntimeException {
    public ScanConflict() { super("SCAN_ID_CONFLICT"); }
}
