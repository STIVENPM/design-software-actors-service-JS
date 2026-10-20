package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.InstructorArea;
import java.util.List;
import java.util.UUID;

/**
 * Write side for instructor areas (E1).
 */
public interface ManageInstructorAreaUseCase {

    InstructorArea create(CreateInstructorAreaCommand cmd, UUID actorId, Viewer viewer);

    void deactivate(UUID instructorId, UUID areaId, UUID actorId, Viewer viewer);

    List<InstructorArea> listByInstructor(UUID instructorId, Viewer viewer);
}
