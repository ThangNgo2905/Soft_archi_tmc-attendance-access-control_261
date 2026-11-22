package com.tmc.aac.features.integration.repository;

import com.tmc.aac.features.integration.repository.HrSyncPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class RestHrSyncClient implements HrSyncPort {

	private final RestClient restClient;

	public RestHrSyncClient(
			RestClient.Builder builder,
			@Value("${tmc.integration.hr.base-url}") String baseUrl) {
		this.restClient = builder.baseUrl(baseUrl).build();
	}

	@Override
	@SuppressWarnings("unchecked")
	public int syncAttendance(List<Map<String, Object>> records) {
		Map<?, ?> body = restClient.post()
				.uri("/api/hr/sync")
				.body(Map.of("records", records))
				.retrieve()
				.body(Map.class);
		if (body == null || body.get("count") == null) {
			return 0;
		}
		return ((Number) body.get("count")).intValue();
	}
}
