package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.QueryLearnerFichaEnrollmentUseCase;
import co.sena.actors.application.port.out.LearnerFichaEnrollmentRepositoryPort;
import co.sena.actors.application.port.out.LearnerRepositoryPort;
import co.sena.actors.domain.model.LearnerFichaEnrollment;
import java.util.List;
import java.util.UUID;

public class QueryLearnerFichaEnrollmentUseCaseImpl implements QueryLearnerFichaEnrollmentUseCase {

    private final LearnerRepositoryPort learnerRepository;
    private final LearnerFichaEnrollmentRepositoryPort enrollmentRepository;

    public QueryLearnerFichaEnrollmentUseCaseImpl(
            LearnerRepositoryPort learnerRepository,
            LearnerFichaEnrollmentRepositoryPort enrollmentRepository) {
        this.learnerRepository = learnerRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public List<LearnerFichaEnrollment> listHistory(UUID learnerId) {
        if (learnerRepository.findById(learnerId).isEmpty()) {
            throw new LearnerNotFoundException("Learner not found with id: " + learnerId);
        }
        return enrollmentRepository.findHistoryByLearnerId(learnerId);
    }
}
