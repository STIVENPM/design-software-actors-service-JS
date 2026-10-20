package co.sena.actors.domain.instructor;

import co.sena.actors.application.port.in.GetActiveCatalogReportUseCase;
import co.sena.actors.application.port.out.CatalogOutputPort;
import co.sena.actors.domain.model.ActiveCatalogReportEntry;

import java.util.List;

public class GetActiveCatalogReportInteractor implements GetActiveCatalogReportUseCase {

    private final CatalogOutputPort catalogOutputPort;

    public GetActiveCatalogReportInteractor(CatalogOutputPort catalogOutputPort) {
        this.catalogOutputPort = catalogOutputPort;
    }

    @Override
    public List<ActiveCatalogReportEntry> getActiveCatalogReport(String catalogCode) {
        return catalogOutputPort.findActiveCatalogReport(catalogCode);
    }
}

