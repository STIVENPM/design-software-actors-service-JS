package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.QueryLearnerUseCase;
import co.sena.actors.application.port.out.LearnerRepositoryPort;
import co.sena.actors.domain.model.Learner;
import java.util.UUID;

public class QueryLearnerUseCaseImpl implements QueryLearnerUseCase {

    private final LearnerRepositoryPort learnerRepository;

    public QueryLearnerUseCaseImpl(LearnerRepositoryPort learnerRepository) {
        this.learnerRepository = learnerRepository;
    }

    @Override
    public Learner getLearnerById(UUID learnerId) {
        return learnerRepository.findById(learnerId)
                .orElseThrow(() -> new LearnerNotFoundException("Learner not found with id: " + learnerId));
    }

    @Override
    public PaginatedResult<Learner> browseLearners(BrowseQuery query) {
        return learnerRepository.findPaginated(query);
    }
}
