package co.sena.actors.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Catalog {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    public Catalog(UUID id, String code, String name, String description, boolean isActive, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return isActive; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

