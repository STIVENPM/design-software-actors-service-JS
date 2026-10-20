package co.sena.actors.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/**
 * POST /instructors/{id}/areas (E1). Source of truth: InstructorAreaCreate from actors.yaml.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record InstructorAreaCreateRequest(
        @NotBlank(message = "area_name is required")
        @Size(max = 100, message = "area_name max 100 characters") String areaName,
        boolean isPrimary) {
}
