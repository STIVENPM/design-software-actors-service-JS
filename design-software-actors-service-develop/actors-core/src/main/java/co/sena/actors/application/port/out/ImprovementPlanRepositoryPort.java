package co.sena.actors.application.port.out;

import co.sena.actors.application.port.in.ManageImprovementPlanUseCase;
import co.sena.actors.domain.model.ActorImprovementPlan;
import java.util.Optional;
import java.util.UUID;

public interface ImprovementPlanRepositoryPort {
    ActorImprovementPlan save(ActorImprovementPlan plan);

    Optional<ActorImprovementPlan> findById(UUID id);

    ManageImprovementPlanUseCase.Page findAll(ManageImprovementPlanUseCase.Query query);

    Optional<StatusInfo> statusInfo(UUID id);

    record StatusInfo(UUID id, boolean terminal) {}
}