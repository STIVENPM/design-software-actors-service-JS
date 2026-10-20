package co.sena.actors.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ActorImprovementPlan(
        UUID id,
        UUID instructorId,
        UUID learnerId,
        String origin,
        UUID originReferenceId,
        String description,
        String specificGoals,
        LocalDate dueDate,
        UUID statusId,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt,
        OffsetDateTime updatedAt,
        UUID updatedBy,
        int rowVersion,
        Boolean isActive,
        OffsetDateTime deletedAt,
        UUID deletedBy) {
}