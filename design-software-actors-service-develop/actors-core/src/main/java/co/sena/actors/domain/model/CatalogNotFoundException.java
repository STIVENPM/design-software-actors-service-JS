package co.sena.actors.domain.model;

import java.util.UUID;

public class CatalogNotFoundException extends RuntimeException {
    public CatalogNotFoundException(UUID catalogId) {
        super("Catalog with id " + catalogId + " not found.");
    }
}

