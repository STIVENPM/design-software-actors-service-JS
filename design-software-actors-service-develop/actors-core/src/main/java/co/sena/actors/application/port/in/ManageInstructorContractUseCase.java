package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.InstructorContract;
import java.util.UUID;

/**
 * Write side for instructor contracts (E2-E3).
 */
public interface ManageInstructorContractUseCase {

    InstructorContract create(CreateInstructorContractCommand cmd, UUID actorId, Viewer viewer);

    InstructorContract update(
            UUID instructorId, UUID contractId, UpdateInstructorContractCommand cmd, UUID actorId, Viewer viewer);
}
