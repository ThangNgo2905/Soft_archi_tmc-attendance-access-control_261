package com.tmc.aac.features.gate.repository;

import com.tmc.aac.features.gate.domain.GateStation;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryGateRepository implements GateRepositoryPort {

	private final Map<UUID, GateStation> store = new ConcurrentHashMap<>();

	@Override
	public List<GateStation> findAll() {
		return new ArrayList<>(store.values());
	}

	@Override
	public Optional<GateStation> findById(UUID id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public GateStation save(GateStation gate) {
		if (gate.getId() == null) {
			gate.setId(UUID.randomUUID());
		}
		store.put(gate.getId(), gate);
		return gate;
	}
}
