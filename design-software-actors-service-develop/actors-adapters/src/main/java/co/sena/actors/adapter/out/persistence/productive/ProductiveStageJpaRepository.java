package co.sena.actors.adapter.out.persistence.productive;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductiveStageJpaRepository extends JpaRepository<ProductiveStageJpaEntity, UUID>,
        JpaSpecificationExecutor<ProductiveStageJpaEntity> {
}
