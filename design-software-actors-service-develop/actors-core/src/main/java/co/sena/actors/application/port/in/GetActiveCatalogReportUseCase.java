package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.ActiveCatalogReportEntry;
import java.util.List;

public interface GetActiveCatalogReportUseCase {
    List<ActiveCatalogReportEntry> getActiveCatalogReport(String catalogCode);
}

