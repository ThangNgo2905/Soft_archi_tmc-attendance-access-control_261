package com.tmc.aac.features.integration.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Component
public class RestTurnstileClient implements TurnstilePort {

	private final RestClient restClient;

	public RestTurnstileClient(
			RestClient.Builder builder,
			@Value("${tmc.integration.turnstile.base-url}") String baseUrl,
			@Value("${tmc.integration.turnstile.connect-timeout-ms:500}") long connectTimeoutMs,
			@Value("${tmc.integration.turnstile.read-timeout-ms:1200}") long readTimeoutMs) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
		requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
		this.restClient = builder
				.baseUrl(baseUrl)
				.requestFactory(requestFactory)
				.build();
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
