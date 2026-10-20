package co.sena.actors.adapter.out.persistence;

import co.sena.actors.application.port.out.CatalogOutputPort;
import co.sena.actors.domain.model.ActiveCatalogReportEntry;
import co.sena.actors.domain.model.Catalog;
import co.sena.actors.domain.model.CatalogDetail;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CatalogPersistenceAdapter implements CatalogOutputPort {

    private final CatalogJpaRepository catalogJpaRepository;
    private final CatalogDetailJpaRepository catalogDetailJpaRepository;

    public CatalogPersistenceAdapter(CatalogJpaRepository catalogJpaRepository, CatalogDetailJpaRepository catalogDetailJpaRepository) {
        this.catalogJpaRepository = catalogJpaRepository;
        this.catalogDetailJpaRepository = catalogDetailJpaRepository;
    }

    @Override
    public boolean existsById(UUID catalogId) {
        return catalogJpaRepository.existsById(catalogId);
    }

    @Override
    public List<Catalog> findCatalogs(String code, Boolean isActive, int page, int size) {
        // ImplementaciÃ³n con paginaciÃ³n y filtros
        return catalogJpaRepository.findByFilters(code, isActive).stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::toDomainCatalog)
                .collect(Collectors.toList());
    }

    @Override
    public List<CatalogDetail> findCatalogDetailsByCatalogId(UUID catalogId, int page, int size) {
        return catalogDetailJpaRepository.findByCatalogId(catalogId).stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::toDomainDetail)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActiveCatalogReportEntry> findActiveCatalogReport(String catalogCode) {
        List<CatalogEntity> catalogs = catalogJpaRepository.findByFilters(catalogCode, true);
        List<UUID> catalogIds = catalogs.stream().map(CatalogEntity::getId).collect(Collectors.toList());

        // Consulta en lote para evitar el problema N+1
        List<CatalogDetailEntity> details = catalogDetailJpaRepository.findActiveByCatalogIds(catalogIds);

        Map<UUID, List<CatalogDetail>> detailsMap = details.stream()
                .map(this::toDomainDetail)
                .collect(Collectors.groupingBy(CatalogDetail::getCatalogId));

        return catalogs.stream()
                .map(c -> new ActiveCatalogReportEntry(
                        toDomainCatalog(c),
                        detailsMap.getOrDefault(c.getId(), List.of())
                ))
                .collect(Collectors.toList());
    }

    private Catalog toDomainCatalog(CatalogEntity entity) {
        return new Catalog(entity.getId(), entity.getCode(), entity.getName(), entity.getDescription(), entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private CatalogDetail toDomainDetail(CatalogDetailEntity entity) {
        return new CatalogDetail(entity.getId(), entity.getCatalogId(), entity.getCode(), entity.getName(), entity.getDescription(), entity.getDisplayOrder(), entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}

