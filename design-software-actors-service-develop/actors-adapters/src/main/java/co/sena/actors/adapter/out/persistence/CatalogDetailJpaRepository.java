package co.sena.actors.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CatalogDetailJpaRepository extends JpaRepository<CatalogDetailEntity, UUID> {

    @Query("SELECT d FROM CatalogDetailEntity d WHERE d.catalogId = :catalogId")
    List<CatalogDetailEntity> findByCatalogId(@Param("catalogId") UUID catalogId);

    @Query("SELECT d FROM CatalogDetailEntity d WHERE d.catalogId IN :catalogIds AND d.isActive = true")
    List<CatalogDetailEntity> findActiveByCatalogIds(@Param("catalogIds") List<UUID> catalogIds);
}

