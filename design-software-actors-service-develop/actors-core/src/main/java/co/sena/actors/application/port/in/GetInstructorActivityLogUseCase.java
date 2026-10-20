package co.sena.actors.application.port.in;

import java.util.UUID;

public interface GetInstructorActivityLogUseCase {

    ActivityLogCursorPage getInstructorActivityLog(UUID instructorId, String cursor, int limit, Viewer viewer);
}
