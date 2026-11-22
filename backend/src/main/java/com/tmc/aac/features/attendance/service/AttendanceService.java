package com.tmc.aac.features.attendance.service;

import com.tmc.aac.features.attendance.domain.AttendanceEvent;
import com.tmc.aac.features.attendance.repository.AttendanceRepositoryPort;
import com.tmc.aac.shared.exception.NotImplementedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttendanceService {

	private final AttendanceRepositoryPort attendanceRepository;

	public AttendanceService(AttendanceRepositoryPort attendanceRepository) {
		this.attendanceRepository = attendanceRepository;
	}

	public List<AttendanceEvent> listRecent(int limit) {
		return attendanceRepository.findRecent(limit);
	}

	public AttendanceEvent recordScan(String qrPayload, String gateCode) {
		throw new NotImplementedException("QR scan → validate → unlock → persist attendance");
	}
}
