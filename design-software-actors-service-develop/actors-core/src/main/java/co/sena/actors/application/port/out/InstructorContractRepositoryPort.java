package co.sena.actors.application.port.out;

import co.sena.actors.domain.model.InstructorContract;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for instructor contracts.
 * Never performs physical DELETE (E2/E3: soft delete).
 */
public interface InstructorContractRepositoryPort {

    InstructorContract save(InstructorContract contract);

    Optional<InstructorContract> findActiveById(UUID id);

    Optional<InstructorContract> findById(UUID id);

    List<InstructorContract> findActiveByInstructorId(UUID instructorId);

    Optional<InstructorContract> findCurrentByInstructorId(UUID instructorId);

    /**
     * Closes the previous current contract and persists the new current one
     * in a single transaction (E2 invariant: one current contract per instructor).
     */
    InstructorContract saveAsCurrent(InstructorContract contract);
}