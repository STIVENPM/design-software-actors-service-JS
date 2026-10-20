package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.CreateCompetencyAssignmentCommand;
import co.sena.actors.application.port.in.ManageCompetencyAssignmentUseCase;
import co.sena.actors.application.port.in.Viewer;
import co.sena.actors.application.port.out.CompetencyAssignmentRepositoryPort;
import co.sena.actors.domain.exception.CompetencyAssignmentNotFoundException;
import co.sena.actors.domain.exception.DuplicateCompetencyAssignmentException;
import co.sena.actors.domain.model.CompetencyAssignment;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Write side for competency assignments (E1-E2).
 */
public class ManageCompetencyAssignmentUseCaseImpl implements ManageCompetencyAssignmentUseCase {

    private final InstructorViewerAccess access;
    private final CompetencyAssignmentRepositoryPort repository;

    public ManageCompetencyAssignmentUseCaseImpl(
            InstructorViewerAccess access, CompetencyAssignmentRepositoryPort repository) {
        this.access = Objects.requireNonNull(access, "access");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public CompetencyAssignment create(CreateCompetencyAssignmentCommand cmd, UUID actorId, Viewer viewer) {
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(actorId, "actorId");
        access.requireVisible(viewer, cmd.instructorId());
        if (repository.existsActiveByInstructorIdAndCompetencyId(cmd.instructorId(), cmd.competencyId())) {
            throw new DuplicateCompetencyAssignmentException(cmd.instructorId(), cmd.competencyId());
        }
        CompetencyAssignment assignment = new CompetencyAssignment();
        assignment.setId(UUID.randomUUID());
        assignment.setInstructorId(cmd.instructorId());
        assignment.setCompetencyId(cmd.competencyId());
        assignment.setCertifiedAt(cmd.certifiedAt());
        assignment.setCertificationEntity(cmd.certificationEntity());
        assignment.setActive(true);
        assignment.setCreatedAt(OffsetDateTime.now());
        assignment.setCreatedBy(actorId);
        assignment.setUpdatedAt(OffsetDateTime.now());
        assignment.setUpdatedBy(actorId);
        assignment.setRowVersion(0);
        return repository.save(assignment);
    }

    @Override
    public void deactivate(UUID instructorId, UUID assignmentId, UUID actorId, Viewer viewer) {
        Objects.requireNonNull(instructorId, "instructorId");
        Objects.requireNonNull(assignmentId, "assignmentId");
        Objects.requireNonNull(actorId, "actorId");
        access.requireVisible(viewer, instructorId);
        CompetencyAssignment assignment = repository.findActiveById(assignmentId)
                .filter(candidate -> instructorId.equals(candidate.getInstructorId()))
                .orElseThrow(() -> new CompetencyAssignmentNotFoundException(assignmentId));
        assignment.setActive(false);
        assignment.setDeletedAt(OffsetDateTime.now());
        assignment.setDeletedBy(actorId);
        assignment.setUpdatedAt(OffsetDateTime.now());
        assignment.setUpdatedBy(actorId);
        repository.save(assignment);
    }
}
