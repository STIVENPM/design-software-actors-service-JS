package co.sena.actors.domain.exception;

import java.util.UUID;

/**
 * 409 CONFLICT: instructor already has this area_name.
 */
public class DuplicateInstructorAreaException extends RuntimeException {

    private final UUID instructorId;
    private final String areaName;

    public DuplicateInstructorAreaException(UUID instructorId, String areaName) {
        super("Instructor " + instructorId + " already has area: " + areaName);
        this.instructorId = instructorId;
        this.areaName = areaName;
    }

    public UUID getInstructorId() {
        return instructorId;
    }

    public String getAreaName() {
        return areaName;
    }

    public String getField() {
        return "area_name";
    }
}