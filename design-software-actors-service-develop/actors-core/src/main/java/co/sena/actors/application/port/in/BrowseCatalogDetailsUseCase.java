package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.CatalogDetail;
import java.util.List;
import java.util.UUID;

public interface BrowseCatalogDetailsUseCase {
    List<CatalogDetail> browseCatalogDetails(UUID catalogId, int page, int size);
}

