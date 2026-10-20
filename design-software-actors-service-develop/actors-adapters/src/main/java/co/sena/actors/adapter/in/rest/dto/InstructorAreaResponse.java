package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.domain.model.InstructorArea;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Instructor area response (E1).
 */
public record InstructorAreaResponse(
        UUID id,
        @JsonProperty("instructor_id") UUID instructorId,
        @JsonProperty("area_name") String areaName,
        @JsonProperty("is_primary") boolean primary,
        @JsonProperty("is_active") boolean active,
        @JsonProperty("created_at") OffsetDateTime createdAt) {

    public static InstructorAreaResponse from(InstructorArea area) {
        return new InstructorAreaResponse(
                area.getId(),
                area.getInstructorId(),
                area.getAreaName(),
                area.isPrimary(),
                area.isActive(),
                area.getCreatedAt());
    }
}
