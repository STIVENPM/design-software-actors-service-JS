package co.sena.actors.adapter.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * PUT /instructors/{id} (E3): full replacement, everything required except phone.
 * No @DecimalMin: a negative value answers 422 (business rule), not 400.
 */
@com.fasterxml.jackson.databind.annotation.JsonNaming(
        com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record InstructorReplaceRequest(
        @NotBlank(message = "document_type is required")
        @Pattern(regexp = "CC|CE|PA|PE", message = "document_type must be CC|CE|PA|PE") String documentType,
        @NotBlank(message = "document_number is required")
        @Size(max = 20, message = "document_number max 20 characters") String documentNumber,
        @NotBlank(message = "first_name is required")
        @Size(max = 100, message = "first_name max 100 characters") String firstName,
        @NotBlank(message = "last_name is required")
        @Size(max = 100, message = "last_name max 100 characters") String lastName,
        @NotBlank(message = "email is required") @Email(message = "invalid email") String email,
        @Size(max = 20, message = "phone max 20 characters") String phone,
        @NotNull(message = "default_max_hours_per_week is required") BigDecimal defaultMaxHoursPerWeek,
        Integer rowVersion) {
}
