package com.tmc.aac.features.integration.service;

import com.tmc.aac.features.integration.repository.HrSyncPort;
import com.tmc.aac.features.integration.repository.TurnstilePort;
import com.tmc.aac.shared.exception.NotImplementedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class IntegrationService {

	private final TurnstilePort turnstilePort;
	private final HrSyncPort hrSyncPort;

	public IntegrationService(TurnstilePort turnstilePort, HrSyncPort hrSyncPort) {
		this.turnstilePort = turnstilePort;
		this.hrSyncPort = hrSyncPort;
	}

	public boolean unlockGate(String gateId, String transactionId) {
		return turnstilePort.unlock(gateId, transactionId);
	}

	public int pushAttendanceToHr(List<Map<String, Object>> records) {
		return hrSyncPort.syncAttendance(records);
	}

	public void scheduleMonthlyExport() {
		throw new NotImplementedException("scheduled monthly HR export before 12:00 on day 1");
	}
}
