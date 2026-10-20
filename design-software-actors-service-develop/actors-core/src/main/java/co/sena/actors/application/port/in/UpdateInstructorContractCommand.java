package co.sena.actors.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Update instructor contract command (E3).
 * Only sent fields are applied; null means no change.
 */
public record UpdateInstructorContractCommand(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal weeklyHourLimit,
        Boolean isCurrent,
        String notes,
        Integer rowVersion) {
}