package co.sena.actors.adapter.in.rest;

import co.sena.actors.application.port.in.BrowseCatalogDetailsUseCase;
import co.sena.actors.application.port.in.BrowseCatalogsUseCase;
import co.sena.actors.application.port.in.GetActiveCatalogReportUseCase;
import co.sena.actors.domain.model.ActiveCatalogReportEntry;
import co.sena.actors.domain.model.Catalog;
import co.sena.actors.domain.model.CatalogDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final BrowseCatalogsUseCase browseCatalogsUseCase;
    private final BrowseCatalogDetailsUseCase browseCatalogDetailsUseCase;
    private final GetActiveCatalogReportUseCase getActiveCatalogReportUseCase;

    public CatalogController(BrowseCatalogsUseCase browseCatalogsUseCase,
                             BrowseCatalogDetailsUseCase browseCatalogDetailsUseCase,
                             GetActiveCatalogReportUseCase getActiveCatalogReportUseCase) {
        this.browseCatalogsUseCase = browseCatalogsUseCase;
        this.browseCatalogDetailsUseCase = browseCatalogDetailsUseCase;
        this.getActiveCatalogReportUseCase = getActiveCatalogReportUseCase;
    }

    @GetMapping("/catalogs")
    @PreAuthorize("hasAuthority('SCOPE_REF_CATALOG_VIEW')")
    public ResponseEntity<List<Catalog>> getCatalogs(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<Catalog> catalogs = browseCatalogsUseCase.browseCatalogs(code, isActive, page, size);
        return ResponseEntity.ok(catalogs);
    }

    @GetMapping("/catalogs/{catalog_id}/details")
    @PreAuthorize("hasAuthority('SCOPE_REF_CATALOG_VIEW')")
    public ResponseEntity<List<CatalogDetail>> getCatalogDetails(
            @PathVariable("catalog_id") UUID catalogId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<CatalogDetail> details = browseCatalogDetailsUseCase.browseCatalogDetails(catalogId, page, size);
        return ResponseEntity.ok(details);
    }

    @GetMapping("/reports/active-catalog")
    @PreAuthorize("hasAuthority('SCOPE_REF_CATALOG_VIEW')")
    public ResponseEntity<List<ActiveCatalogReportEntry>> getActiveCatalogReport(
            @RequestParam(required = false) String catalogCode) {
        List<ActiveCatalogReportEntry> report = getActiveCatalogReportUseCase.getActiveCatalogReport(catalogCode);
        return ResponseEntity.ok(report);
    }
}

