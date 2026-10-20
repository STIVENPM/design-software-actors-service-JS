package co.sena.actors.adapter.out.persistence.productive;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyVisitJpaRepository extends JpaRepository<CompanyVisitJpaEntity, UUID> {
    long countByProductiveStageId(UUID productiveStageId);
}
