package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.domain.model.LearnerEnrollmentStage;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LearnerFichaEnrollmentCreateRequest(
        @NotNull(message = "ficha_id is required") UUID fichaId,
        @NotNull(message = "enrollment_status_id is required") UUID enrollmentStatusId,
        @NotNull(message = "stage is required") LearnerEnrollmentStage stage,
        @NotNull(message = "enrollment_date is required") LocalDate enrollmentDate,
        String transferReason) {
}
