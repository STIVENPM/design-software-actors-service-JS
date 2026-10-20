package co.sena.actors.domain.exception;

import java.util.UUID;

/**
 * 409 CONFLICT: optimistic locking conflict on instructor area.
 */
public class StaleInstructorAreaVersionException extends RuntimeException {

    private final UUID areaId;

    public StaleInstructorAreaVersionException(UUID areaId) {
        super("Concurrency conflict: stale row_version for instructor area " + areaId);
        this.areaId = areaId;
    }

    public UUID getAreaId() {
        return areaId;
    }
}