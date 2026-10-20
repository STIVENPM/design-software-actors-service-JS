package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.domain.model.ProductiveStage;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductiveStageResponse(
        UUID id,
        @JsonProperty("learner_id") UUID learnerId,
        @JsonProperty("company_id") UUID companyId,
        @JsonProperty("supervisor_instructor_id") UUID supervisorInstructorId,
        @JsonProperty("status_id") UUID statusId,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("planned_end_date") LocalDate plannedEndDate,
        @JsonProperty("total_hours_required") Integer totalHoursRequired,
        @JsonProperty("total_hours_completed") Integer totalHoursCompleted,
        @JsonProperty("interruption_reason") String interruptionReason,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt) {
    public static ProductiveStageResponse from(ProductiveStage stage) {
        return new ProductiveStageResponse(stage.id(), stage.learnerId(), stage.companyId(),
                stage.supervisorInstructorId(), stage.statusId(), stage.startDate(),
                stage.plannedEndDate(), stage.totalHoursRequired(), stage.totalHoursCompleted(),
                stage.interruptionReason(), stage.createdAt(), stage.updatedAt());
    }
}
