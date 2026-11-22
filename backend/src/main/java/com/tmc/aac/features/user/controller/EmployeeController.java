package com.tmc.aac.features.user.controller;

import com.tmc.aac.shared.api.ApiResponse;
import com.tmc.aac.features.user.service.EmployeeService;
import com.tmc.aac.features.user.domain.Employee;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employees", description = "User and QR code management (scaffold)")
public class EmployeeController {

	private final EmployeeService employeeService;

	public EmployeeController(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	@GetMapping
	@Operation(summary = "List employees")
	public ApiResponse<List<Employee>> list() {
		return ApiResponse.ok(employeeService.listEmployees());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get employee by id")
	public ApiResponse<Employee> get(@PathVariable UUID id) {
		return ApiResponse.ok(employeeService.getEmployee(id));
	}
}
