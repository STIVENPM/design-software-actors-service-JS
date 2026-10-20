package co.sena.actors.core.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class InstructorAvailabilityException {

    private UUID id;
    private UUID instructorId;
    private String exceptionType;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String description;
    private UUID approvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;
    private LocalDateTime deletedAt;
    private UUID deletedBy;
    private UUID createdBy;
    private UUID updatedBy;
    private Integer rowVersion;

    public InstructorAvailabilityException() {}

    public InstructorAvailabilityException(
            UUID id,
            UUID instructorId,
            String exceptionType,
            LocalDateTime startDatetime,
            LocalDateTime endDatetime,
            String description,
            UUID approvedBy,
            LocalDateTime createdAt,
            boolean active,
            LocalDateTime deletedAt,
            UUID deletedBy) {
        this(id, instructorId, exceptionType, startDatetime, endDatetime, description,
                approvedBy, createdAt, null, active, deletedAt, deletedBy, null, null, null);
    }

    public InstructorAvailabilityException(
            UUID id,
            UUID instructorId,
            String exceptionType,
            LocalDateTime startDatetime,
            LocalDateTime endDatetime,
            String description,
            UUID approvedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            boolean active,
            LocalDateTime deletedAt,
            UUID deletedBy,
            UUID createdBy,
            UUID updatedBy,
            Integer rowVersion) {
        this.id = id;
        this.instructorId = instructorId;
        this.exceptionType = exceptionType;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.description = description;
        this.approvedBy = approvedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.active = active;
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.rowVersion = rowVersion;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getInstructorId() { return instructorId; }
    public void setInstructorId(UUID instructorId) { this.instructorId = instructorId; }

    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }

    public LocalDateTime getStartDatetime() { return startDatetime; }
    public void setStartDatetime(LocalDateTime startDatetime) { this.startDatetime = startDatetime; }

    public LocalDateTime getEndDatetime() { return endDatetime; }
    public void setEndDatetime(LocalDateTime endDatetime) { this.endDatetime = endDatetime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getApprovedBy() { return approvedBy; }
    public void setApprovedBy(UUID approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public UUID getDeletedBy() { return deletedBy; }
    public void setDeletedBy(UUID deletedBy) { this.deletedBy = deletedBy; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public Integer getRowVersion() { return rowVersion; }
    public void setRowVersion(Integer rowVersion) { this.rowVersion = rowVersion; }
}
