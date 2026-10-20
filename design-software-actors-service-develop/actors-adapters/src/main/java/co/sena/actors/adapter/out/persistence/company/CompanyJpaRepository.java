package co.sena.actors.adapter.out.persistence.company;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CompanyJpaRepository extends JpaRepository<CompanyJpaEntity, UUID>,
        JpaSpecificationExecutor<CompanyJpaEntity> {
    boolean existsByNit(String nit);
    Optional<CompanyJpaEntity> findByIdAndActiveTrue(UUID id);
}
