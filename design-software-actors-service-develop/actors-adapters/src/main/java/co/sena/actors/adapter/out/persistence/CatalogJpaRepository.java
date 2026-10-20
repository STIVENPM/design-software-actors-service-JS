package co.sena.actors.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CatalogJpaRepository extends JpaRepository<CatalogEntity, UUID> {

    @Query("SELECT c FROM CatalogEntity c WHERE (:code IS NULL OR c.code = :code) AND (:isActive IS NULL OR c.isActive = :isActive)")
    List<CatalogEntity> findByFilters(@Param("code") String code, @Param("isActive") Boolean isActive);
}

