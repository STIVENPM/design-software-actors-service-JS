package co.sena.actors.domain.exception;

import java.util.UUID;

/** 409 CONFLICT: the sent row_version does not match (optimistic locking). */
public class StaleInstructorVersionException extends RuntimeException {

    private final UUID instructorId;

    public StaleInstructorVersionException(UUID instructorId) {
        super("Instructor concurrency conflict: " + instructorId);
        this.instructorId = instructorId;
    }

    public UUID getInstructorId() {
        return instructorId;
    }
}
