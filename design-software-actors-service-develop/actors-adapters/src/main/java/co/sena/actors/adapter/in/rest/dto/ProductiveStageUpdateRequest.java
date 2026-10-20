package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.application.port.in.ProductiveStageUseCase;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProductiveStageUpdateRequest(
        UUID statusId,
        @PositiveOrZero Integer totalHoursCompleted,
        String interruptionReason,
        LocalDate plannedEndDate) {
    public ProductiveStageUseCase.UpdateCommand toCommand() {
        return new ProductiveStageUseCase.UpdateCommand(statusId, totalHoursCompleted,
                interruptionReason, plannedEndDate);
    }
}
