package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.application.port.in.ProductiveStageUseCase;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CompanyVisitCreateRequest(
        @NotNull @Positive Integer visitNumber,
        @NotNull LocalDate visitDate,
        @NotNull String visitType,
        @NotNull String learnerPerformance,
        String companySatisfaction,
        Boolean hasImprovementPlan,
        UUID instructorId) {
    public ProductiveStageUseCase.VisitCommand toCommand() {
        return new ProductiveStageUseCase.VisitCommand(visitNumber, visitDate, visitType,
                learnerPerformance, companySatisfaction, hasImprovementPlan, instructorId);
    }
}
