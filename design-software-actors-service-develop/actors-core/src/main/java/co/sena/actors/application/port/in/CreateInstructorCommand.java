package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.DocumentType;
import java.math.BigDecimal;
import java.util.UUID;

/** Administrative creation command (E1). user_id: logical reference to iam-service. */
public record CreateInstructorCommand(
        UUID userId,
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        BigDecimal defaultMaxHoursPerWeek) {
}
