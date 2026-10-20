package co.sena.actors.adapter.in.rest.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * POST /instructors/{id}/competencies (E1).
 * Source of truth: CompetencyAssignmentCreate from actors.yaml.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CompetencyAssignmentCreateRequest(
        @NotNull(message = "competency_id is required") UUID competencyId,
        @NotNull(message = "certified_at is required") LocalDate certifiedAt,
        @Size(max = 200, message = "certification_entity max 200 characters") String certificationEntity) {
}
