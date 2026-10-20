package co.sena.actors.domain.model;

import java.util.List;

public class ActiveCatalogReportEntry {
    private Catalog catalog;
    private List<CatalogDetail> activeDetails;

    public ActiveCatalogReportEntry(Catalog catalog, List<CatalogDetail> activeDetails) {
        this.catalog = catalog;
        this.activeDetails = activeDetails;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public List<CatalogDetail> getActiveDetails() {
        return activeDetails;
    }
}

