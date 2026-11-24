package com.tmc.aac.features.gate.repository;

import com.tmc.aac.features.gate.domain.GateDirection;
import com.tmc.aac.features.gate.domain.GateStation;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Primary
public class JpaGateRepositoryAdapter implements GateRepositoryPort {

	private final SpringDataGateJpaRepository jpaRepository;

	public JpaGateRepositoryAdapter(SpringDataGateJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public List<GateStation> findAll() {
		return jpaRepository.findAll().stream()
				.map(this::toDomain)
				.toList();
	}

	@Override
	public Optional<GateStation> findById(UUID id) {
		return jpaRepository.findById(id).map(this::toDomain);
	}

	@Override
	public Optional<GateStation> findByCode(String code) {
		return jpaRepository.findByCode(code).map(this::toDomain);
	}

	@Override
	public GateStation save(GateStation gate) {
		GateJpaEntity entity = toEntity(gate);
		GateJpaEntity saved = jpaRepository.save(entity);
		return toDomain(saved);
	}

	private GateStation toDomain(GateJpaEntity entity) {
		GateStation domain = new GateStation();
		domain.setId(entity.getId());
		domain.setCode(entity.getCode());
		domain.setName(entity.getName());
		domain.setFactoryCode(entity.getFactoryCode());
		domain.setDirection(entity.getDirection());
		domain.setActive(entity.isActive());
		return domain;
	}

	private GateJpaEntity toEntity(GateStation domain) {
		GateJpaEntity entity = new GateJpaEntity();
		entity.setId(domain.getId() != null ? domain.getId() : UUID.randomUUID());
		entity.setCode(domain.getCode());
		entity.setName(domain.getName());
		entity.setFactoryCode(domain.getFactoryCode());
		entity.setDirection(domain.getDirection() != null ? domain.getDirection() : GateDirection.IN);
		entity.setActive(domain.isActive());
		entity.setCreatedAt(Instant.now());
		return entity;
	}
}
