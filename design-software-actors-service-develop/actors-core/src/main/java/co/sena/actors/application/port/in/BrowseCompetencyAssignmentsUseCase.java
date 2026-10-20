package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.CompetencyAssignment;
import java.util.List;
import java.util.UUID;

/**
 * Read side for competency assignments (E2). scheduling-service consumes
 * the active list to enforce RN-ACT-02 on its side.
 */
public interface BrowseCompetencyAssignmentsUseCase {

    List<CompetencyAssignment> listActiveByInstructor(UUID instructorId, Viewer viewer);
}
