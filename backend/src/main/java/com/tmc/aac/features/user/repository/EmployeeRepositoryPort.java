package com.tmc.aac.features.user.repository;

import com.tmc.aac.features.user.domain.Employee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepositoryPort {

	List<Employee> findAll();

	Optional<Employee> findById(UUID id);

	Employee save(Employee employee);
}
