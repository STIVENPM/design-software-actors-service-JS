package co.sena.actors.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Competency assignment (table actors_instructors.competency_assignment).
 * Certifies that an instructor may teach a competency (logical reference
 * to academic-management-service, no physical FK). scheduling-service
 * consumes the active list to validate session assignments (RN-ACT-02,
 * enforced there, not here).
 */
public class CompetencyAssignment {

    private UUID id;
    private UUID instructorId;
    private UUID competencyId;
    private LocalDate certifiedAt;
    private String certificationEntity;
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

    public UUID getCompetencyId() {
        return competencyId;
    }

    public void setCompetencyId(UUID competencyId) {
        this.competencyId = competencyId;
    }

    public LocalDate getCertifiedAt() {
        return certifiedAt;
    }

    public void setCertifiedAt(LocalDate certifiedAt) {
        this.certifiedAt = certifiedAt;
    }

    public String getCertificationEntity() {
        return certificationEntity;
    }

    public void setCertificationEntity(String certificationEntity) {
        this.certificationEntity = certificationEntity;
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
