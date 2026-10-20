package co.sena.actors.application.port.out;

import co.sena.actors.domain.model.ActivityLogEntry;

import java.util.List;
import java.util.UUID;

public interface ActivityLogRepositoryPort {

    void save(ActivityLogEntry entry);

    List<ActivityLogEntry> findByInstructorIdWithCursor(UUID instructorId, String cursor, int limit);

    List<ActivityLogEntry> findByLearnerIdWithCursor(UUID learnerId, String cursor, int limit);
}
