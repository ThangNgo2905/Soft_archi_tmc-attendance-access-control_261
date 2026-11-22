package com.tmc.aac.features.user.service;

import com.tmc.aac.shared.exception.NotImplementedException;
import com.tmc.aac.shared.exception.ResourceNotFoundException;
import com.tmc.aac.features.user.domain.Employee;
import com.tmc.aac.features.user.repository.EmployeeRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EmployeeService {

	private final EmployeeRepositoryPort employeeRepository;

	public EmployeeService(EmployeeRepositoryPort employeeRepository) {
		this.employeeRepository = employeeRepository;
	}

	public List<Employee> listEmployees() {
		return employeeRepository.findAll();
	}

	public Employee getEmployee(UUID id) {
		return employeeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Employee", id));
	}

	public Employee createEmployee(Employee employee) {
		throw new NotImplementedException("create employee / QR issuance");
	}
}
