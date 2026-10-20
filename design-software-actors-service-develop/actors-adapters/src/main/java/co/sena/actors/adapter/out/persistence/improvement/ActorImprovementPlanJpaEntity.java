package co.sena.actors.adapter.out.persistence.improvement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "actor_improvement_plan", schema = "actors_tracking")
public class ActorImprovementPlanJpaEntity {
    @Id
    private UUID id;
    @Column(name = "instructor_id") private UUID instructorId;
    @Column(name = "learner_id") private UUID learnerId;
    @Column(name = "origin", nullable = false) private String origin;
    @Column(name = "origin_reference_id") private UUID originReferenceId;
    @Column(name = "description", nullable = false, columnDefinition = "text") private String description;
    @Column(name = "specific_goals", nullable = false, columnDefinition = "text") private String specificGoals;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(name = "status_id", nullable = false) private UUID statusId;
    @Column(name = "created_by", nullable = false, updatable = false) private UUID createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "completed_at") private OffsetDateTime completedAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "updated_by", nullable = false) private UUID updatedBy;
    @Column(name = "is_active", nullable = false) private Boolean isActive = true;
    @Column(name = "deleted_at") private OffsetDateTime deletedAt;
    @Column(name = "deleted_by") private UUID deletedBy;
    @Version
    @Column(name = "row_version", nullable = false)
    private int rowVersion;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getInstructorId() { return instructorId; }
    public void setInstructorId(UUID instructorId) { this.instructorId = instructorId; }
    public UUID getLearnerId() { return learnerId; }
    public void setLearnerId(UUID learnerId) { this.learnerId = learnerId; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public UUID getOriginReferenceId() { return originReferenceId; }
    public void setOriginReferenceId(UUID originReferenceId) { this.originReferenceId = originReferenceId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSpecificGoals() { return specificGoals; }
    public void setSpecificGoals(String specificGoals) { this.specificGoals = specificGoals; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public UUID getStatusId() { return statusId; }
    public void setStatusId(UUID statusId) { this.statusId = statusId; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public int getRowVersion() { return rowVersion; }
    public void setRowVersion(int rowVersion) { this.rowVersion = rowVersion; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
    public UUID getDeletedBy() { return deletedBy; }
    public void setDeletedBy(UUID deletedBy) { this.deletedBy = deletedBy; }
}