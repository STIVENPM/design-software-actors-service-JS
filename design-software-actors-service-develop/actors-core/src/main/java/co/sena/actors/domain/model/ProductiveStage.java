package co.sena.actors.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductiveStage(
        UUID id,
        UUID learnerId,
        UUID companyId,
        UUID supervisorInstructorId,
        UUID statusId,
        LocalDate startDate,
        LocalDate plannedEndDate,
        Integer totalHoursRequired,
        Integer totalHoursCompleted,
        String interruptionReason,
        OffsetDateTime createdAt,
        UUID createdBy,
        OffsetDateTime updatedAt,
        UUID updatedBy,
        int rowVersion) {
}
