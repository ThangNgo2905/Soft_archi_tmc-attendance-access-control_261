package com.tmc.aac.features.integration.repository;

import java.util.List;
import java.util.Map;

public interface HrSyncPort {

	int syncAttendance(List<Map<String, Object>> records);
}
