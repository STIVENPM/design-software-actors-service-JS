package co.sena.actors.application.port.out;

import co.sena.actors.application.port.in.ManageLearnerFichaEnrollmentUseCase.CreateCommand;
import co.sena.actors.domain.model.LearnerFichaEnrollment;
import java.util.List;
import java.util.UUID;

public interface LearnerFichaEnrollmentRepositoryPort {

    LearnerFichaEnrollment createEnrollment(UUID learnerId, CreateCommand command);

    List<LearnerFichaEnrollment> findHistoryByLearnerId(UUID learnerId);

    List<UUID> findActiveFichaIdsByLearnerId(UUID learnerId);

    long countActiveLearnersByFichaId(UUID fichaId);
}
