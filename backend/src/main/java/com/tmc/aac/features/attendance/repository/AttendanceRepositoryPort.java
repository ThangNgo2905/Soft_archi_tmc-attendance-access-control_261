package com.tmc.aac.features.attendance.repository;

import com.tmc.aac.features.attendance.domain.AttendanceEvent;

import java.util.List;

public interface AttendanceRepositoryPort {

	List<AttendanceEvent> findRecent(int limit);

	AttendanceEvent save(AttendanceEvent event);
}
