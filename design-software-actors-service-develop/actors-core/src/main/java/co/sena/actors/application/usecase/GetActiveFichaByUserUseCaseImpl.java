package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.GetActiveFichaByUserUseCase;
import co.sena.actors.application.port.out.LearnerFichaEnrollmentRepositoryPort;
import co.sena.actors.application.port.out.LearnerRepositoryPort;
import co.sena.actors.domain.model.Learner;
import java.util.List;
import java.util.UUID;

public class GetActiveFichaByUserUseCaseImpl
        implements GetActiveFichaByUserUseCase {

    private final LearnerRepositoryPort learnerRepository;
    private final LearnerFichaEnrollmentRepositoryPort enrollmentRepository;

    public GetActiveFichaByUserUseCaseImpl(
            LearnerRepositoryPort learnerRepository,
            LearnerFichaEnrollmentRepositoryPort enrollmentRepository) {
        this.learnerRepository = learnerRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public ActiveFichaResult findActiveFichaByUserId(UUID userId) {
        Learner learner = learnerRepository.findActiveByUserId(userId)
                .orElseThrow(() ->
                        new LearnerNotFoundException(
                                "Learner not found for user_id: " + userId));

        List<UUID> activeFichaIds =
                enrollmentRepository.findActiveFichaIdsByLearnerId(learner.id());

        if (activeFichaIds.size() > 1) {
            throw new ActiveFichaInconsistencyException(
                    "More than one active ficha found for learner: " + learner.id());
        }

        UUID fichaId = activeFichaIds.isEmpty()
                ? null
                : activeFichaIds.get(0);

        return new ActiveFichaResult(learner.id(), fichaId);
    }
}