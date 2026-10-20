package co.sena.actors.domain.exception;

import java.util.UUID;

/** 404 NOT_FOUND: availability exception does not exist or is already inactive. */
public class InstructorAvailabilityExceptionNotFoundException extends RuntimeException {

    private final UUID exceptionId;
    private final UUID instructorId;

    public InstructorAvailabilityExceptionNotFoundException(UUID exceptionId, UUID instructorId) {
        super("Availability exception not found for instructor " + instructorId + ": " + exceptionId);
        this.exceptionId = exceptionId;
        this.instructorId = instructorId;
    }

    public UUID getExceptionId() {
        return exceptionId;
    }

    public UUID getInstructorId() {
        return instructorId;
    }
}
