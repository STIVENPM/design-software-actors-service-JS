package co.sena.actors.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyVisit(
        UUID id,
        UUID productiveStageId,
        UUID instructorId,
        Integer visitNumber,
        LocalDate visitDate,
        String visitType,
        String learnerPerformance,
        String companySatisfaction,
        Boolean hasImprovementPlan,
        Boolean firstVisitLate,
        OffsetDateTime createdAt,
        UUID createdBy) {
}
