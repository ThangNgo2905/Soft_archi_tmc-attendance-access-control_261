package com.tmc.aac.features.gate.controller;

import com.tmc.aac.features.gate.service.GateService;
import com.tmc.aac.features.gate.domain.GateStation;
import com.tmc.aac.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gates")
@Tag(name = "Gates", description = "Gate station management (scaffold)")
public class GateController {

	private final GateService gateService;

	public GateController(GateService gateService) {
		this.gateService = gateService;
	}

	@GetMapping
	@Operation(summary = "List gate stations")
	public ApiResponse<List<GateStation>> list() {
		return ApiResponse.ok(gateService.listGates());
	}
}
