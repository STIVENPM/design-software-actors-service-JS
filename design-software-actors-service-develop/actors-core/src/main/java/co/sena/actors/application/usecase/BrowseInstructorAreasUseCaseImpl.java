package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.BrowseInstructorAreasUseCase;
import co.sena.actors.application.port.in.Viewer;
import co.sena.actors.application.port.out.InstructorAreaRepositoryPort;
import co.sena.actors.domain.model.InstructorArea;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Read side for instructor areas (E1).
 */
public class BrowseInstructorAreasUseCaseImpl implements BrowseInstructorAreasUseCase {

    private final InstructorViewerAccess access;
    private final InstructorAreaRepositoryPort repository;

    public BrowseInstructorAreasUseCaseImpl(
            InstructorViewerAccess access, InstructorAreaRepositoryPort repository) {
        this.access = Objects.requireNonNull(access, "access");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public List<InstructorArea> listActiveByInstructor(UUID instructorId, Viewer viewer) {
        access.requireVisible(viewer, instructorId);
        return repository.findActiveByInstructorId(instructorId);
    }
}
