package co.sena.actors.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/**
 * POST /instructors/{id}/contracts (E2). Source of truth: InstructorContractCreate from actors.yaml.
 * weekly_hour_limit: Límite específico del contrato; fuente autoritativa del límite vigente.
 * RN-ACT-01 (STAFF=40h, CONTRACTOR<=48h, HOURLY sin límite) se valida en capa de aplicación.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record InstructorContractCreateRequest(
        @NotNull(message = "contract_type is required") String contractType,
        @NotNull(message = "start_date is required") LocalDate startDate,
        LocalDate endDate,
        @NotNull(message = "training_center_id is required") UUID trainingCenterId,
        BigDecimal weeklyHourLimit,
        @Size(max = 1000, message = "notes max 1000 characters") String notes,
        boolean isCurrent) {
}
