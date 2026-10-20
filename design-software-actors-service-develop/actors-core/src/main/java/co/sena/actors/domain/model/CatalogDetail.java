package co.sena.actors.domain.model;

import java.time.Instant;
import java.util.UUID;

public class CatalogDetail {
    private UUID id;
    private UUID catalogId;
    private String code;
    private String name;
    private String description;
    private Integer displayOrder;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    public CatalogDetail(UUID id, UUID catalogId, String code, String name, String description, Integer displayOrder, boolean isActive, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.catalogId = catalogId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getCatalogId() { return catalogId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return isActive; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

