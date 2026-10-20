package co.sena.actors.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record InstructorLoadReportEntry(
        UUID instructorId,
        String documentNumber,
        String fullName,
        BigDecimal defaultMaxHoursPerWeek,
        BigDecimal currentContractWeeklyHourLimit,
        UUID trainingCenterId,
        int activeAreasCount,
        int activeCompetenciesCount,
        int activeExceptionsCount) {
}