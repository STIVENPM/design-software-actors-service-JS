package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.CountActiveLearnersByFichaUseCase;
import co.sena.actors.application.port.out.FichaExistencePort;
import co.sena.actors.application.port.out.LearnerFichaEnrollmentRepositoryPort;
import java.util.Objects;
import java.util.UUID;

public class CountActiveLearnersByFichaUseCaseImpl implements CountActiveLearnersByFichaUseCase {

    private final LearnerFichaEnrollmentRepositoryPort enrollments;
    private final FichaExistencePort fichas;

    public CountActiveLearnersByFichaUseCaseImpl(
            LearnerFichaEnrollmentRepositoryPort enrollments, FichaExistencePort fichas) {
        this.enrollments = Objects.requireNonNull(enrollments);
        this.fichas = Objects.requireNonNull(fichas);
    }

    @Override
    public long countActiveLearners(UUID fichaId) {
        Objects.requireNonNull(fichaId, "fichaId");
        if (!fichas.existsById(fichaId)) {
            throw new FichaNotFoundException();
        }
        return enrollments.countActiveLearnersByFichaId(fichaId);
    }
}
