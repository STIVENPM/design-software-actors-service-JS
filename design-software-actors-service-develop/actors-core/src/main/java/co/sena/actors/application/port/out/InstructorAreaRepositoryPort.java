package co.sena.actors.application.port.out;

import co.sena.actors.domain.model.InstructorArea;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for instructor areas.
 * Never performs physical DELETE (E1: soft delete).
 */
public interface InstructorAreaRepositoryPort {

    InstructorArea save(InstructorArea area);

    Optional<InstructorArea> findActiveById(UUID id);

    Optional<InstructorArea> findById(UUID id);

    List<InstructorArea> findActiveByInstructorId(UUID instructorId);

    boolean existsByInstructorIdAndAreaName(UUID instructorId, String areaName);

    boolean existsByInstructorIdAndAreaNameExcludingId(UUID instructorId, String areaName, UUID excludeId);
}