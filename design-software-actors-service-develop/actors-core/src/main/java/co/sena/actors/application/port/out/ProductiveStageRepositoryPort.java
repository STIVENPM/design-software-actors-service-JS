package co.sena.actors.application.port.out;

import co.sena.actors.application.port.in.ProductiveStageUseCase;
import co.sena.actors.domain.model.CompanyVisit;
import co.sena.actors.domain.model.ProductiveStage;
import java.util.Optional;
import java.util.UUID;

public interface ProductiveStageRepositoryPort {
    ProductiveStage save(ProductiveStage stage);
    Optional<ProductiveStage> findById(UUID id);
    ProductiveStageUseCase.Page findAll(ProductiveStageUseCase.Query query);
    CompanyVisit saveVisit(CompanyVisit visit);
    long countVisits(UUID productiveStageId);
    boolean existsLearner(UUID id);
    boolean existsActiveCompany(UUID id);
    boolean existsActiveInstructor(UUID id);
    Optional<StatusInfo> statusInfo(UUID id);

    record StatusInfo(UUID id, String code, boolean terminal) {}
}
