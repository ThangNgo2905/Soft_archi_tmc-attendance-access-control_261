package com.tmc.aac.features.attendance.repository;

import com.tmc.aac.features.attendance.domain.AttendanceEvent;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryAttendanceRepository implements AttendanceRepositoryPort {

	private final List<AttendanceEvent> store = new CopyOnWriteArrayList<>();

	@Override
	public List<AttendanceEvent> findRecent(int limit) {
		return store.stream()
				.sorted(Comparator.comparing(AttendanceEvent::getOccurredAt,
						Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(Math.max(limit, 0))
				.toList();
	}

	@Override
	public AttendanceEvent save(AttendanceEvent event) {
		if (event.getId() == null) {
			event.setId(UUID.randomUUID());
		}
		store.add(event);
		return event;
	}
}
