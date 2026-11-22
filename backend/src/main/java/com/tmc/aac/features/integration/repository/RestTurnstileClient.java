package com.tmc.aac.features.integration.repository;

import com.tmc.aac.features.integration.repository.TurnstilePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class RestTurnstileClient implements TurnstilePort {

	private final RestClient restClient;

	public RestTurnstileClient(
			RestClient.Builder builder,
			@Value("${tmc.integration.turnstile.base-url}") String baseUrl) {
		this.restClient = builder.baseUrl(baseUrl).build();
	}

	@Override
	public boolean unlock(String gateId, String transactionId) {
		Map<?, ?> body = restClient.post()
				.uri("/api/turnstile/unlock")
				.body(Map.of("gateId", gateId, "transactionId", transactionId))
				.retrieve()
				.body(Map.class);
		return body != null && Boolean.TRUE.equals(body.get("success"));
	}
}
