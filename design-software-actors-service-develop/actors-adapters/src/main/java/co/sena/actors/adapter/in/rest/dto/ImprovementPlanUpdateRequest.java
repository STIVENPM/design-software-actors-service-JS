package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.application.port.in.ManageImprovementPlanUseCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.UUID;

public record ImprovementPlanUpdateRequest(
        String description,
        @JsonProperty("specific_goals") String specificGoals,
        @JsonProperty("due_date") LocalDate dueDate,
        @JsonProperty("status_id") UUID statusId) {
    public ManageImprovementPlanUseCase.UpdateCommand toCommand() {
        return new ManageImprovementPlanUseCase.UpdateCommand(description, specificGoals, dueDate, statusId);
    }
}