package com.tmc.aac.features.user.repository;

import com.tmc.aac.features.user.domain.Employee;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory adapter kept for unit tests and non-JPA contexts.
 */
@Repository
public class InMemoryEmployeeRepository implements EmployeeRepositoryPort {

	private final Map<UUID, Employee> store = new ConcurrentHashMap<>();

	@Override
	public List<Employee> findAll() {
		return new ArrayList<>(store.values());
	}

	@Override
	public Optional<Employee> findById(UUID id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public Optional<Employee> findByEmployeeCode(String employeeCode) {
		return store.values().stream()
				.filter(e -> employeeCode != null && employeeCode.equals(e.getEmployeeCode()))
				.findFirst();
	}

	@Override
	public Employee save(Employee employee) {
		if (employee.getId() == null) {
			employee.setId(UUID.randomUUID());
		}
		store.put(employee.getId(), employee);
		return employee;
	}
}
