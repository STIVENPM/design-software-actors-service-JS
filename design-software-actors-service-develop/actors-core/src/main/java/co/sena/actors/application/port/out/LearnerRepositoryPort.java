package co.sena.actors.application.port.out;

import co.sena.actors.application.port.in.QueryLearnerUseCase.BrowseQuery;
import co.sena.actors.application.port.in.QueryLearnerUseCase.PaginatedResult;
import co.sena.actors.domain.model.Learner;
import java.util.Optional;
import java.util.UUID;

public interface LearnerRepositoryPort {

    Learner save(Learner learner);

    Optional<Learner> findById(UUID id);

    Optional<Learner> findByUserId(UUID userId);

    Optional<Learner> findActiveByUserId(UUID userId);

    boolean existsByDocumentNumber(String documentNumber);

    PaginatedResult<Learner> findPaginated(BrowseQuery query);
}
