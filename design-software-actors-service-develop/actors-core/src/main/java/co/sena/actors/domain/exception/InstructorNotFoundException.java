package co.sena.actors.domain.exception;

import java.util.UUID;

/** 404 NOT_FOUND: it does not exist or is soft-deleted (is_active=false). */
public class InstructorNotFoundException extends RuntimeException {

    private final UUID instructorId;

    public InstructorNotFoundException(UUID instructorId) {
        super("Instructor not found: " + instructorId);
        this.instructorId = instructorId;
    }

    public UUID getInstructorId() {
        return instructorId;
    }
}
