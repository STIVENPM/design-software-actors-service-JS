package co.sena.actors.domain.exception;

import java.util.UUID;

/**
 * 404 NOT_FOUND: competency assignment not found or soft-deleted.
 */
public class CompetencyAssignmentNotFoundException extends RuntimeException {

    private final UUID assignmentId;

    public CompetencyAssignmentNotFoundException(UUID assignmentId) {
        super("Competency assignment not found: " + assignmentId);
        this.assignmentId = assignmentId;
    }

    public UUID getAssignmentId() {
        return assignmentId;
    }
}
