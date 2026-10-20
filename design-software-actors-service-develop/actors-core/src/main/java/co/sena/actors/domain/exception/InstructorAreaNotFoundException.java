package co.sena.actors.domain.exception;

import java.util.UUID;

/**
 * 404 NOT_FOUND: instructor area not found or soft-deleted.
 */
public class InstructorAreaNotFoundException extends RuntimeException {

    private final UUID areaId;

    public InstructorAreaNotFoundException(UUID areaId) {
        super("Instructor area not found: " + areaId);
        this.areaId = areaId;
    }

    public UUID getAreaId() {
        return areaId;
    }
}