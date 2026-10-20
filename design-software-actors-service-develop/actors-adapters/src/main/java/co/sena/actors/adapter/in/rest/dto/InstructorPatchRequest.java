package co.sena.actors.adapter.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * PATCH /instructors/{id} (E3): only sent fields are applied.
 * No @DecimalMin: a negative value answers 422 (business rule), not 400.
 */
@com.fasterxml.jackson.databind.annotation.JsonNaming(
        com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record InstructorPatchRequest(
        @Pattern(regexp = "CC|CE|PA|PE", message = "document_type must be CC|CE|PA|PE") String documentType,
        @Size(max = 20, message = "document_number max 20 characters") String documentNumber,
        @Size(max = 100, message = "first_name max 100 characters") String firstName,
        @Size(max = 100, message = "last_name max 100 characters") String lastName,
        @Email(message = "invalid email") String email,
        @Size(max = 20, message = "phone max 20 characters") String phone,
        BigDecimal defaultMaxHoursPerWeek,
        Integer rowVersion) {
}
