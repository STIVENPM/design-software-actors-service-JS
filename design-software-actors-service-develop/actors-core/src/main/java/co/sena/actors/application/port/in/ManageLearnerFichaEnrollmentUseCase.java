package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.LearnerEnrollmentStage;
import co.sena.actors.domain.model.LearnerFichaEnrollment;
import java.time.LocalDate;
import java.util.UUID;

public interface ManageLearnerFichaEnrollmentUseCase {

    LearnerFichaEnrollment enroll(UUID learnerId, CreateCommand command);

    record CreateCommand(
            UUID fichaId,
            UUID enrollmentStatusId,
            LearnerEnrollmentStage stage,
            LocalDate enrollmentDate,
            String transferReason) {
    }
}
