package co.sena.actors.adapter.in.rest.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/**
 * PUT /instructors/{id}/contracts/{contract_id} (E3). Source of truth: InstructorContractUpdate from actors.yaml.
 * Only sent fields are applied.
 * No @DecimalMin: negative value answers 422 (business rule), not 400.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record InstructorContractUpdateRequest(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal weeklyHourLimit,
        Boolean isCurrent,
        @Size(max = 1000, message = "notes max 1000 characters") String notes,
        Integer rowVersion) {
}
