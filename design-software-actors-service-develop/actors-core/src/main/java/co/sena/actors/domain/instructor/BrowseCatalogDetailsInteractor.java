package co.sena.actors.domain.instructor;

import co.sena.actors.application.port.in.BrowseCatalogDetailsUseCase;
import co.sena.actors.application.port.out.CatalogOutputPort;
import co.sena.actors.domain.model.CatalogNotFoundException;
import co.sena.actors.domain.model.CatalogDetail;

import java.util.List;
import java.util.UUID;

public class BrowseCatalogDetailsInteractor implements BrowseCatalogDetailsUseCase {

    private final CatalogOutputPort catalogOutputPort;

    public BrowseCatalogDetailsInteractor(CatalogOutputPort catalogOutputPort) {
        this.catalogOutputPort = catalogOutputPort;
    }

    @Override
    public List<CatalogDetail> browseCatalogDetails(UUID catalogId, int page, int size) {
        if (!catalogOutputPort.existsById(catalogId)) {
            throw new CatalogNotFoundException(catalogId);
        }
        return catalogOutputPort.findCatalogDetailsByCatalogId(catalogId, page, size);
    }
}

