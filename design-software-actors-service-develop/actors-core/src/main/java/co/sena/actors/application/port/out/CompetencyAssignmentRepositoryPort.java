package co.sena.actors.application.port.out;

import co.sena.actors.domain.model.CompetencyAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for competency assignments.
 * Never performs physical DELETE (E2: soft delete).
 */
public interface CompetencyAssignmentRepositoryPort {

    CompetencyAssignment save(CompetencyAssignment assignment);

    Optional<CompetencyAssignment> findActiveById(UUID id);

    Optional<CompetencyAssignment> findById(UUID id);

    List<CompetencyAssignment> findActiveByInstructorId(UUID instructorId);

    boolean existsActiveByInstructorIdAndCompetencyId(UUID instructorId, UUID competencyId);
}
