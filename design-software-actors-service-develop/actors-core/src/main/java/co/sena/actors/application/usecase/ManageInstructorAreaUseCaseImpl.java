package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.CreateInstructorAreaCommand;
import co.sena.actors.application.port.in.ManageInstructorAreaUseCase;
import co.sena.actors.application.port.in.Viewer;
import co.sena.actors.application.port.out.InstructorAreaRepositoryPort;
import co.sena.actors.domain.exception.DuplicateInstructorAreaException;
import co.sena.actors.domain.exception.InstructorAreaNotFoundException;
import co.sena.actors.domain.model.InstructorArea;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Write side for instructor areas (E1).
 */
public class ManageInstructorAreaUseCaseImpl implements ManageInstructorAreaUseCase {

    private final InstructorViewerAccess access;
    private final InstructorAreaRepositoryPort repository;

    public ManageInstructorAreaUseCaseImpl(
            InstructorViewerAccess access, InstructorAreaRepositoryPort repository) {
        this.access = Objects.requireNonNull(access, "access");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public InstructorArea create(CreateInstructorAreaCommand cmd, UUID actorId, Viewer viewer) {
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(actorId, "actorId");
        access.requireVisible(viewer, cmd.instructorId());
        if (repository.existsByInstructorIdAndAreaName(cmd.instructorId(), cmd.areaName())) {
            throw new DuplicateInstructorAreaException(cmd.instructorId(), cmd.areaName());
        }
        InstructorArea area = new InstructorArea();
        area.setId(UUID.randomUUID());
        area.setInstructorId(cmd.instructorId());
        area.setAreaName(cmd.areaName());
        area.setPrimary(cmd.primary());
        area.setActive(true);
        area.setCreatedAt(OffsetDateTime.now());
        area.setCreatedBy(actorId);
        area.setUpdatedAt(OffsetDateTime.now());
        area.setUpdatedBy(actorId);
        area.setRowVersion(0);
        return repository.save(area);
    }

    @Override
    public void deactivate(UUID instructorId, UUID areaId, UUID actorId, Viewer viewer) {
        Objects.requireNonNull(instructorId, "instructorId");
        Objects.requireNonNull(areaId, "areaId");
        Objects.requireNonNull(actorId, "actorId");
        access.requireVisible(viewer, instructorId);
        InstructorArea area = repository.findActiveById(areaId)
                .filter(candidate -> instructorId.equals(candidate.getInstructorId()))
                .orElseThrow(() -> new InstructorAreaNotFoundException(areaId));
        area.setActive(false);
        area.setDeletedAt(OffsetDateTime.now());
        area.setDeletedBy(actorId);
        area.setUpdatedAt(OffsetDateTime.now());
        area.setUpdatedBy(actorId);
        repository.save(area);
    }

    @Override
    public List<InstructorArea> listByInstructor(UUID instructorId, Viewer viewer) {
        access.requireVisible(viewer, instructorId);
        return repository.findActiveByInstructorId(instructorId);
    }
}
