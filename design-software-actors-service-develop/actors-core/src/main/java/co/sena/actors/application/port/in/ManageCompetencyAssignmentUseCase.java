package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.CompetencyAssignment;
import java.util.UUID;

/**
 * Write side for competency assignments (E1-E2).
 */
public interface ManageCompetencyAssignmentUseCase {

    CompetencyAssignment create(CreateCompetencyAssignmentCommand cmd, UUID actorId, Viewer viewer);

    void deactivate(UUID instructorId, UUID assignmentId, UUID actorId, Viewer viewer);
}
