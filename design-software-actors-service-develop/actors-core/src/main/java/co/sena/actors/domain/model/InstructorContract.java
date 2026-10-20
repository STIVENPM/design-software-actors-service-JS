package co.sena.actors.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Instructor contract (table actors_instructors.instructor_contract).
 * RN-ACT-01 (STAFF=40h, CONTRACTOR<=48h, HOURLY no límite) se valida en capa de aplicación, no en DDL.
 * weekly_hour_limit es la fuente autoritativa del límite vigente.
 */
public class InstructorContract {

    private UUID id;
    private UUID instructorId;
    private ContractType contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private UUID trainingCenterId;
    private BigDecimal weeklyHourLimit;
    private boolean current;
    private String notes;
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

    public ContractType getContractType() {
        return contractType;
    }

    public void setContractType(ContractType contractType) {
        this.contractType = contractType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public UUID getTrainingCenterId() {
        return trainingCenterId;
    }

    public void setTrainingCenterId(UUID trainingCenterId) {
        this.trainingCenterId = trainingCenterId;
    }

    public BigDecimal getWeeklyHourLimit() {
        return weeklyHourLimit;
    }

    public void setWeeklyHourLimit(BigDecimal weeklyHourLimit) {
        this.weeklyHourLimit = weeklyHourLimit;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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