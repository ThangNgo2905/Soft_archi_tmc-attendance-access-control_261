package com.tmc.aac.shared.controller;

import com.tmc.aac.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "System", description = "System administration stubs")
public class HealthInfoController {

	@GetMapping("/meta")
	@Operation(summary = "API metadata")
	public ApiResponse<Map<String, String>> meta() {
		return ApiResponse.ok(Map.of(
				"name", "TMC AAC",
				"version", "0.0.1-SNAPSHOT",
				"phase", "structure-scaffold"
		));
	}
}
