package com.tmc.aac.features.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataEmployeeJpaRepository extends JpaRepository<EmployeeJpaEntity, UUID> {

	Optional<EmployeeJpaEntity> findByEmployeeCode(String employeeCode);
}
