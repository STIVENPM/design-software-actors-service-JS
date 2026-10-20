package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.LearnerFichaEnrollment;
import java.util.List;
import java.util.UUID;

public interface QueryLearnerFichaEnrollmentUseCase {

    List<LearnerFichaEnrollment> listHistory(UUID learnerId);
}
