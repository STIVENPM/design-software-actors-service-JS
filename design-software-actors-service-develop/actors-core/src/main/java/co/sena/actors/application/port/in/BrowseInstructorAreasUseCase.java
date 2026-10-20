package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.InstructorArea;
import java.util.List;
import java.util.UUID;

/**
 * Read side for instructor areas (E1).
 */
public interface BrowseInstructorAreasUseCase {

    List<InstructorArea> listActiveByInstructor(UUID instructorId, Viewer viewer);
}
