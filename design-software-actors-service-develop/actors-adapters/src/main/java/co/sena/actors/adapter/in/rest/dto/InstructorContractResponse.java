package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.domain.model.InstructorContract;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Instructor contract response (E2-E3).
 */
public record InstructorContractResponse(
        UUID id,
        @JsonProperty("instructor_id") UUID instructorId,
        @JsonProperty("contract_type") String contractType,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate,
        @JsonProperty("training_center_id") UUID trainingCenterId,
        @JsonProperty("weekly_hour_limit") BigDecimal weeklyHourLimit,
        @JsonProperty("is_current") boolean current,
        String notes,
        @JsonProperty("is_active") boolean active,
        @JsonProperty("row_version") int rowVersion,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt) {

    public static InstructorContractResponse from(InstructorContract contract) {
        return new InstructorContractResponse(
                contract.getId(),
                contract.getInstructorId(),
                contract.getContractType() == null ? null : contract.getContractType().name(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getTrainingCenterId(),
                contract.getWeeklyHourLimit(),
                contract.isCurrent(),
                contract.getNotes(),
                contract.isActive(),
                contract.getRowVersion(),
                contract.getCreatedAt(),
                contract.getUpdatedAt());
    }
}
