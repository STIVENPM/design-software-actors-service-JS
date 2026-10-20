package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.domain.model.CompetencyAssignment;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Competency assignment response (E1-E2).
 */
public record CompetencyAssignmentResponse(
        UUID id,
        @JsonProperty("instructor_id") UUID instructorId,
        @JsonProperty("competency_id") UUID competencyId,
        @JsonProperty("certified_at") LocalDate certifiedAt,
        @JsonProperty("certification_entity") String certificationEntity,
        @JsonProperty("is_active") boolean active,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt) {

    public static CompetencyAssignmentResponse from(CompetencyAssignment assignment) {
        return new CompetencyAssignmentResponse(
                assignment.getId(),
                assignment.getInstructorId(),
                assignment.getCompetencyId(),
                assignment.getCertifiedAt(),
                assignment.getCertificationEntity(),
                assignment.isActive(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }
}
