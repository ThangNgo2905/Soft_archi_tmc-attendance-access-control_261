package com.tmc.aac.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("TMC Attendance & Access Control API")
						.description("Scaffold API for QR-based attendance and gate access control.")
						.version("v1"));
	}
}
