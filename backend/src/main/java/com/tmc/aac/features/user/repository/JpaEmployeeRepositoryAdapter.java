package com.tmc.aac.features.user.repository;

import com.tmc.aac.features.user.domain.Employee;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Primary
public class JpaEmployeeRepositoryAdapter implements EmployeeRepositoryPort {

	private final SpringDataEmployeeJpaRepository jpaRepository;

	public JpaEmployeeRepositoryAdapter(SpringDataEmployeeJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public List<Employee> findAll() {
		return jpaRepository.findAll().stream()
				.map(this::toDomain)
				.toList();
	}

	@Override
	public Optional<Employee> findById(UUID id) {
		return jpaRepository.findById(id).map(this::toDomain);
	}

	@Override
	public Optional<Employee> findByEmployeeCode(String employeeCode) {
		return jpaRepository.findByEmployeeCode(employeeCode).map(this::toDomain);
	}

	@Override
	public Employee save(Employee employee) {
		EmployeeJpaEntity entity = toEntity(employee);
		EmployeeJpaEntity saved = jpaRepository.save(entity);
		return toDomain(saved);
	}

	private Employee toDomain(EmployeeJpaEntity entity) {
		Employee domain = new Employee();
		domain.setId(entity.getId());
		domain.setEmployeeCode(entity.getEmployeeCode());
		domain.setFullName(entity.getFullName());
		domain.setFactoryCode(entity.getFactoryCode());
		domain.setActive(entity.isActive());
		return domain;
	}

	private EmployeeJpaEntity toEntity(Employee domain) {
		EmployeeJpaEntity entity = new EmployeeJpaEntity();
		entity.setId(domain.getId() != null ? domain.getId() : UUID.randomUUID());
		entity.setEmployeeCode(domain.getEmployeeCode());
		entity.setFullName(domain.getFullName());
		entity.setFactoryCode(domain.getFactoryCode());
		entity.setActive(domain.isActive());
		entity.setCreatedAt(Instant.now());
		return entity;
	}
}
