package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.BrowseInstructorContractsUseCase;
import co.sena.actors.application.port.in.Viewer;
import co.sena.actors.application.port.out.InstructorContractRepositoryPort;
import co.sena.actors.domain.exception.InstructorContractNotFoundException;
import co.sena.actors.domain.model.InstructorContract;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Read side for instructor contracts (E3).
 */
public class BrowseInstructorContractsUseCaseImpl implements BrowseInstructorContractsUseCase {

    private final InstructorViewerAccess access;
    private final InstructorContractRepositoryPort repository;

    public BrowseInstructorContractsUseCaseImpl(
            InstructorViewerAccess access, InstructorContractRepositoryPort repository) {
        this.access = Objects.requireNonNull(access, "access");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public InstructorContract get(UUID instructorId, UUID contractId, Viewer viewer) {
        access.requireVisible(viewer, instructorId);
        return repository.findActiveById(contractId)
                .filter(candidate -> instructorId.equals(candidate.getInstructorId()))
                .orElseThrow(() -> new InstructorContractNotFoundException(contractId));
    }

    @Override
    public List<InstructorContract> listByInstructor(UUID instructorId, Viewer viewer) {
        access.requireVisible(viewer, instructorId);
        return repository.findActiveByInstructorId(instructorId);
    }
}
