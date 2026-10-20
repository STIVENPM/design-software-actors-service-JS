package co.sena.actors.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Instructor technological area (table actors_instructors.instructor_area).
 */
public class InstructorArea {

    private UUID id;
    private UUID instructorId;
    private String areaName;
    private boolean primary;
    private boolean active = true;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;
    private OffsetDateTime deletedAt;
    private UUID deletedBy;
    private int rowVersion;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(UUID instructorId) {
        this.instructorId = instructorId;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public UUID getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(UUID deletedBy) {
        this.deletedBy = deletedBy;
    }

    public int getRowVersion() {
        return rowVersion;
    }

    public void setRowVersion(int rowVersion) {
        this.rowVersion = rowVersion;
    }
}