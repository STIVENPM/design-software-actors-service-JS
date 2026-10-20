package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.DocumentType;
import java.math.BigDecimal;

/**
 * PATCH: only sent fields are applied (null = no change).
 * Optional rowVersion for optimistic locking (409 on mismatch).
 */
public record PatchInstructorCommand(
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        BigDecimal defaultMaxHoursPerWeek,
        Integer rowVersion) {
}
