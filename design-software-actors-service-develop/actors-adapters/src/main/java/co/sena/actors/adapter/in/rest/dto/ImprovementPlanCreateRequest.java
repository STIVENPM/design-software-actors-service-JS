package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.application.port.in.ManageImprovementPlanUseCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ImprovementPlanCreateRequest(
        @JsonProperty("instructor_id") UUID instructorId,
        @JsonProperty("learner_id") UUID learnerId,
        @NotBlank String origin,
        @JsonProperty("origin_reference_id") UUID originReferenceId,
        @NotBlank String description,
        @JsonProperty("specific_goals") @NotBlank String specificGoals,
        @JsonProperty("due_date") @NotNull LocalDate dueDate,
        @JsonProperty("status_id") @NotNull UUID statusId) {
    public ManageImprovementPlanUseCase.CreateCommand toCommand() {
        return new ManageImprovementPlanUseCase.CreateCommand(instructorId, learnerId, origin,
                originReferenceId, description, specificGoals, dueDate, statusId);
    }
}