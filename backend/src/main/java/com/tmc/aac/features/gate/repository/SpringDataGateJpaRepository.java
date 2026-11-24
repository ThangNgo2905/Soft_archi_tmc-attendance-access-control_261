package com.tmc.aac.features.gate.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataGateJpaRepository extends JpaRepository<GateJpaEntity, UUID> {

	Optional<GateJpaEntity> findByCode(String code);
}
