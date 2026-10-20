package co.sena.actors.domain.instructor;

import co.sena.actors.application.port.in.BrowseCatalogsUseCase;
import co.sena.actors.application.port.out.CatalogOutputPort;
import co.sena.actors.domain.model.Catalog;

import java.util.List;

public class BrowseCatalogsInteractor implements BrowseCatalogsUseCase {

    private final CatalogOutputPort catalogOutputPort;

    public BrowseCatalogsInteractor(CatalogOutputPort catalogOutputPort) {
        this.catalogOutputPort = catalogOutputPort;
    }

    @Override
    public List<Catalog> browseCatalogs(String code, Boolean isActive, int page, int size) {
        return catalogOutputPort.findCatalogs(code, isActive, page, size);
    }
}

