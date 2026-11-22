package com.tmc.aac.features.reporting.service;

import com.tmc.aac.shared.exception.NotImplementedException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ReportingService {

	public Map<String, Object> monthlySummary(String yearMonth) {
		throw new NotImplementedException("monthly attendance report for " + yearMonth);
	}
}
