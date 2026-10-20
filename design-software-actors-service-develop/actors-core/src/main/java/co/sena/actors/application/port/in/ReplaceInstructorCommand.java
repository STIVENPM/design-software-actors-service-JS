package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.DocumentType;
import java.math.BigDecimal;

/**
 * PUT: full resource replacement. Every field is required (except phone).
 * rowVersion is optional: when sent and mismatched, the API answers
 * 409 CONFLICT (optimistic locking).
 */
public record ReplaceInstructorCommand(
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        BigDecimal defaultMaxHoursPerWeek,
        Integer rowVersion) {
}
