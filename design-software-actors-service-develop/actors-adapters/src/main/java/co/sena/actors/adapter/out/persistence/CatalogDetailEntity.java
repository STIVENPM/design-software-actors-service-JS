package co.sena.actors.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_detail", schema = "parameterization")
public class CatalogDetailEntity {

    @Id
    private UUID id;

    @Column(name = "catalog_id", nullable = false)
    private UUID catalogId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Getters y setters bÃ¡sicos
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

