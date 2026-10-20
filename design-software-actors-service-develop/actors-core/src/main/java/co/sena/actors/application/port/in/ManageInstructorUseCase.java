package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.Instructor;
import java.util.UUID;

/** Base instructor profile use case (HU-ACT-001, E1-E4). */
public interface ManageInstructorUseCase {

    Instructor create(CreateInstructorCommand command, UUID actorUuid);

    Instructor getActive(UUID id);

    Instructor replace(UUID id, ReplaceInstructorCommand command, UUID actorUuid);

    Instructor patch(UUID id, PatchInstructorCommand command, UUID actorUuid);

    void deactivate(UUID id, UUID actorUuid);
}
