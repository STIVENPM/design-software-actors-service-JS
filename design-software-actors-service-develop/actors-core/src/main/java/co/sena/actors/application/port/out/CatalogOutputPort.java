package co.sena.actors.application.port.out;

import co.sena.actors.domain.model.ActiveCatalogReportEntry;
import co.sena.actors.domain.model.Catalog;
import co.sena.actors.domain.model.CatalogDetail;

import java.util.List;
import java.util.UUID;

public interface CatalogOutputPort {

    boolean existsById(UUID catalogId);

    List<Catalog> findCatalogs(String code, Boolean isActive, int page, int size);

    List<CatalogDetail> findCatalogDetailsByCatalogId(UUID catalogId, int page, int size);

    List<ActiveCatalogReportEntry> findActiveCatalogReport(String catalogCode);
}

