package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.InstructorContract;
import java.util.List;
import java.util.UUID;

/**
 * Read side for instructor contracts (E3).
 */
public interface BrowseInstructorContractsUseCase {

    InstructorContract get(UUID instructorId, UUID contractId, Viewer viewer);

    List<InstructorContract> listByInstructor(UUID instructorId, Viewer viewer);
}
