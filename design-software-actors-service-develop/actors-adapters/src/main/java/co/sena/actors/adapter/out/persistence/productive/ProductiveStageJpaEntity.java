package co.sena.actors.adapter.out.persistence.productive;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "productive_stage", schema = "actors_companies")
public class ProductiveStageJpaEntity {
    @Id
    private UUID id;

    @Column(name = "learner_id", nullable = false)
    private UUID learnerId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "supervisor_instructor_id", nullable = false)
    private UUID supervisorInstructorId;

    @Column(name = "status_id", nullable = false)
    private UUID statusId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "total_hours_required", nullable = false)
    private Integer totalHoursRequired;

    @Column(name = "total_hours_completed", nullable = false)
    private Integer totalHoursCompleted;

    @Column(name = "interruption_reason", columnDefinition = "text")
    private String interruptionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Version
    @Column(name = "row_version", nullable = false)
    private int rowVersion;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getLearnerId() { return learnerId; }
    public void setLearnerId(UUID learnerId) { this.learnerId = learnerId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getSupervisorInstructorId() { return supervisorInstructorId; }
    public void setSupervisorInstructorId(UUID supervisorInstructorId) { this.supervisorInstructorId = supervisorInstructorId; }
    public UUID getStatusId() { return statusId; }
    public void setStatusId(UUID statusId) { this.statusId = statusId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getPlannedEndDate() { return plannedEndDate; }
    public void setPlannedEndDate(LocalDate plannedEndDate) { this.plannedEndDate = plannedEndDate; }
    public Integer getTotalHoursRequired() { return totalHoursRequired; }
    public void setTotalHoursRequired(Integer totalHoursRequired) { this.totalHoursRequired = totalHoursRequired; }
    public Integer getTotalHoursCompleted() { return totalHoursCompleted; }
    public void setTotalHoursCompleted(Integer totalHoursCompleted) { this.totalHoursCompleted = totalHoursCompleted; }
    public String getInterruptionReason() { return interruptionReason; }
    public void setInterruptionReason(String interruptionReason) { this.interruptionReason = interruptionReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public int getRowVersion() { return rowVersion; }
    public void setRowVersion(int rowVersion) { this.rowVersion = rowVersion; }
}
