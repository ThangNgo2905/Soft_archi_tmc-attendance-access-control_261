package com.tmc.aac.features.gate.service;

import com.tmc.aac.features.gate.domain.GateStation;
import com.tmc.aac.features.gate.repository.GateRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GateService {

	private final GateRepositoryPort gateRepository;

	public GateService(GateRepositoryPort gateRepository) {
		this.gateRepository = gateRepository;
	}

	public List<GateStation> listGates() {
		return gateRepository.findAll();
	}
}
