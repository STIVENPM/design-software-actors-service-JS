package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.Instructor;
import java.util.UUID;

/**
 * Read side of the instructor base profile (E2). Pagination, filters and
 * visibility live here, never in the REST adapter. Until HU-ACT-002 only
 * GLOBAL (or bare feature) and OWN_PROFILE scopes are supported; any other
 * scope (e.g. TRAINING_CENTER alone) is rejected so rows are never leaked.
 */
public interface BrowseInstructorsUseCase {

    Instructor get(UUID id, Viewer viewer);

    InstructorPage list(InstructorQuery query, Viewer viewer);
}
