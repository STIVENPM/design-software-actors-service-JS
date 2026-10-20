package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.ContractType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Create instructor contract command (E2).
 * When isCurrent=true, the previous current contract for the same instructor is closed.
 */
public record CreateInstructorContractCommand(
        UUID instructorId,
        ContractType contractType,
        LocalDate startDate,
        LocalDate endDate,
        UUID trainingCenterId,
        BigDecimal weeklyHourLimit,
        String notes,
        boolean isCurrent) {
}