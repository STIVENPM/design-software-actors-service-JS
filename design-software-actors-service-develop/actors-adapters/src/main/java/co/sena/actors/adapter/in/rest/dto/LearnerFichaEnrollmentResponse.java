package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.domain.model.LearnerFichaEnrollment;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.LocalDate;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LearnerFichaEnrollmentResponse(
        UUID id,
        UUID learnerId,
        UUID fichaId,
        UUID enrollmentStatusId,
        String stage,
        LocalDate enrollmentDate,
        LocalDate completionDate,
        boolean isCurrent,
        String transferReason) {

    public static LearnerFichaEnrollmentResponse from(LearnerFichaEnrollment enrollment) {
        return new LearnerFichaEnrollmentResponse(
                enrollment.id(),
                enrollment.learnerId(),
                enrollment.fichaId(),
                enrollment.enrollmentStatusId(),
                enrollment.stage().name(),
                enrollment.enrollmentDate(),
                enrollment.completionDate(),
                enrollment.isCurrent(),
                enrollment.transferReason());
    }
}
