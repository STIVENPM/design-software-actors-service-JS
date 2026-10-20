package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.BrowseCompetencyAssignmentsUseCase;
import co.sena.actors.application.port.in.Viewer;
import co.sena.actors.application.port.out.CompetencyAssignmentRepositoryPort;
import co.sena.actors.domain.model.CompetencyAssignment;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Read side for competency assignments (E2).
 */
public class BrowseCompetencyAssignmentsUseCaseImpl implements BrowseCompetencyAssignmentsUseCase {

    private final InstructorViewerAccess access;
    private final CompetencyAssignmentRepositoryPort repository;

    public BrowseCompetencyAssignmentsUseCaseImpl(
            InstructorViewerAccess access, CompetencyAssignmentRepositoryPort repository) {
        this.access = Objects.requireNonNull(access, "access");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public List<CompetencyAssignment> listActiveByInstructor(UUID instructorId, Viewer viewer) {
        access.requireVisible(viewer, instructorId);
        return repository.findActiveByInstructorId(instructorId);
    }
}
