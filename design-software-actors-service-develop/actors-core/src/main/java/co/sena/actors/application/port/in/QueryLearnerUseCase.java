package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.Learner;
import java.util.List;
import java.util.UUID;

public interface QueryLearnerUseCase {

    Learner getLearnerById(UUID learnerId);

    PaginatedResult<Learner> browseLearners(BrowseQuery query);

    record BrowseQuery(
            String documentNumber,
            UUID fichaId,
            UUID enrollmentStatusId,
            int page,
            int pageSize) {}
            
    record PaginatedResult<T>(
            List<T> data,
            int totalItems,
            int totalPages,
            int currentPage) {}
}
