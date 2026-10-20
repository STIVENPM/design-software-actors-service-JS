package co.sena.actors.domain.exception;

import java.util.UUID;

/**
 * 409 CONFLICT: the instructor already has an active assignment for this competency.
 */
public class DuplicateCompetencyAssignmentException extends RuntimeException {

    private final UUID instructorId;
    private final UUID competencyId;

    public DuplicateCompetencyAssignmentException(UUID instructorId, UUID competencyId) {
        super("Instructor " + instructorId + " already assigned to competency: " + competencyId);
        this.instructorId = instructorId;
        this.competencyId = competencyId;
    }

    public UUID getInstructorId() {
        return instructorId;
    }

    public UUID getCompetencyId() {
        return competencyId;
    }

    public String getField() {
        return "competency_id";
    }
}
