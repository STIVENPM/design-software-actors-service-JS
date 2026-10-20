package co.sena.actors.application.port.in;

import java.util.UUID;

public interface GetLearnerActivityLogUseCase {

    ActivityLogCursorPage getLearnerActivityLog(UUID learnerId, String cursor, int limit, Viewer viewer);
}
