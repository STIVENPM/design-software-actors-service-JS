package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.application.port.in.ProductiveStageUseCase;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProductiveStageCreateRequest(
        @NotNull UUID learnerId,
        @NotNull UUID companyId,
        @NotNull UUID supervisorInstructorId,
        @NotNull LocalDate startDate,
        LocalDate plannedEndDate,
        @NotNull @Positive Integer totalHoursRequired,
        @NotNull UUID statusId) {
    public ProductiveStageUseCase.CreateCommand toCommand() {
        return new ProductiveStageUseCase.CreateCommand(learnerId, companyId,
                supervisorInstructorId, startDate, plannedEndDate, totalHoursRequired, statusId);
    }
}
