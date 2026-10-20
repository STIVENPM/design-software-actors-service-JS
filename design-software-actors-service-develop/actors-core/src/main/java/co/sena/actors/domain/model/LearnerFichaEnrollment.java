package co.sena.actors.domain.model;

import java.time.LocalDate;
import java.util.UUID;

public record LearnerFichaEnrollment(
        UUID id,
        UUID learnerId,
        UUID fichaId,
        UUID enrollmentStatusId,
        LearnerEnrollmentStage stage,
        LocalDate enrollmentDate,
        LocalDate completionDate,
        boolean isCurrent,
        String transferReason) {
}
