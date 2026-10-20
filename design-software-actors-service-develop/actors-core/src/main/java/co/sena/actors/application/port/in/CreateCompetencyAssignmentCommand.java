package co.sena.actors.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Assign competency command (E1).
 */
public record CreateCompetencyAssignmentCommand(
        UUID instructorId,
        UUID competencyId,
        LocalDate certifiedAt,
        String certificationEntity) {
}
