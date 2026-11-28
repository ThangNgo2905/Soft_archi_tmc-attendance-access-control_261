package com.tmc.aac.configuration;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	/**
	 * Prototype scope: each HTTP adapter gets its own builder, so calls like
	 * {@code baseUrl(...)} or {@code requestFactory(...)} in one adapter do not leak into another.
	 */
	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}
}
