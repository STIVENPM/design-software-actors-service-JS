package co.sena.actors.adapter.out.persistence.improvement;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActorImprovementPlanJpaRepository
        extends JpaRepository<ActorImprovementPlanJpaEntity, UUID>,
        JpaSpecificationExecutor<ActorImprovementPlanJpaEntity> {
}