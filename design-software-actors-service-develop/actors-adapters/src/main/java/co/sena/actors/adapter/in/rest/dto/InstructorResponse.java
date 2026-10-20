package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.domain.model.Instructor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Instructor response (E1-E3). full_name is readOnly and derived,
 * never persisted. row_version exposes optimistic locking.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InstructorResponse(
        UUID id,
        @JsonProperty("user_id") UUID userId,
        @JsonProperty("document_type") String documentType,
        @JsonProperty("document_number") String documentNumber,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty(value = "full_name", access = JsonProperty.Access.READ_ONLY) String fullName,
        String email,
        String phone,
        @JsonProperty("default_max_hours_per_week") BigDecimal defaultMaxHoursPerWeek,
        @JsonProperty("is_active") boolean active,
        @JsonProperty("row_version") int rowVersion,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt) {

    public static InstructorResponse from(Instructor instructor) {
        return new InstructorResponse(
                instructor.getId(),
                instructor.getUserId(),
                instructor.getDocumentType() == null ? null : instructor.getDocumentType().name(),
                instructor.getDocumentNumber(),
                instructor.getFirstName(),
                instructor.getLastName(),
                instructor.getFullName(),
                instructor.getEmail(),
                instructor.getPhone(),
                instructor.getDefaultMaxHoursPerWeek(),
                instructor.isActive(),
                instructor.getRowVersion(),
                instructor.getCreatedAt(),
                instructor.getUpdatedAt());
    }
}
