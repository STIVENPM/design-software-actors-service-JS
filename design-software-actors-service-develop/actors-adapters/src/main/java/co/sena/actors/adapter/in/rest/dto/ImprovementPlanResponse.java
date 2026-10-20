package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.domain.model.ActorImprovementPlan;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ImprovementPlanResponse(
        UUID id,
        @JsonProperty("instructor_id") UUID instructorId,
        @JsonProperty("learner_id") UUID learnerId,
        String origin,
        @JsonProperty("origin_reference_id") UUID originReferenceId,
        String description,
        @JsonProperty("specific_goals") String specificGoals,
        @JsonProperty("due_date") LocalDate dueDate,
        @JsonProperty("status_id") UUID statusId,
        @JsonProperty("created_by") UUID createdBy,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("completed_at") OffsetDateTime completedAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt) {
    public static ImprovementPlanResponse from(ActorImprovementPlan plan) {
        return new ImprovementPlanResponse(plan.id(), plan.instructorId(), plan.learnerId(), plan.origin(),
                plan.originReferenceId(), plan.description(), plan.specificGoals(), plan.dueDate(), plan.statusId(),
                plan.createdBy(), plan.createdAt(), plan.completedAt(), plan.updatedAt());
    }
}