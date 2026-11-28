package com.tmc.aac.features.attendance.domain;

public enum AttendanceStatus {
	/** Validated online by the backend; turnstile unlocked. */
	GRANTED,
	/** Recorded by an Edge Kiosk while offline and synced later (Store-and-Forward). */
	SYNCED_OFFLINE
}
