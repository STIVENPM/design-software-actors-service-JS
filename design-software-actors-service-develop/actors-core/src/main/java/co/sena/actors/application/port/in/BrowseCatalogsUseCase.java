package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.Catalog;
import java.util.List;

public interface BrowseCatalogsUseCase {
    List<Catalog> browseCatalogs(String code, Boolean isActive, int page, int size);
}

