package com.tmc.aac.features.gate.repository;

import com.tmc.aac.features.gate.domain.GateStation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GateRepositoryPort {

	List<GateStation> findAll();

	Optional<GateStation> findById(UUID id);

	Optional<GateStation> findByCode(String code);

	GateStation save(GateStation gate);
}
