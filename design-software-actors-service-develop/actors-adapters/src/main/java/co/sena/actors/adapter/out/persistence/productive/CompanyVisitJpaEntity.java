package co.sena.actors.adapter.out.persistence.productive;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_visit", schema = "actors_companies")
public class CompanyVisitJpaEntity {
    @Id
    private UUID id;

    @Column(name = "productive_stage_id", nullable = false)
    private UUID productiveStageId;

    @Column(name = "instructor_id", nullable = false)
    private UUID instructorId;

    @Column(name = "visit_number", nullable = false)
    private Integer visitNumber;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "visit_type", nullable = false, length = 20)
    private String visitType;

    @Column(name = "learner_performance", nullable = false, length = 20)
    private String learnerPerformance;

    @Column(name = "company_satisfaction", length = 20)
    private String companySatisfaction;

    @Column(name = "has_improvement_plan", nullable = false)
    private Boolean hasImprovementPlan;

    @Column(name = "first_visit_late", nullable = false)
    private Boolean firstVisitLate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProductiveStageId() { return productiveStageId; }
    public void setProductiveStageId(UUID productiveStageId) { this.productiveStageId = productiveStageId; }
    public UUID getInstructorId() { return instructorId; }
    public void setInstructorId(UUID instructorId) { this.instructorId = instructorId; }
    public Integer getVisitNumber() { return visitNumber; }
    public void setVisitNumber(Integer visitNumber) { this.visitNumber = visitNumber; }
    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }
    public String getVisitType() { return visitType; }
    public void setVisitType(String visitType) { this.visitType = visitType; }
    public String getLearnerPerformance() { return learnerPerformance; }
    public void setLearnerPerformance(String learnerPerformance) { this.learnerPerformance = learnerPerformance; }
    public String getCompanySatisfaction() { return companySatisfaction; }
    public void setCompanySatisfaction(String companySatisfaction) { this.companySatisfaction = companySatisfaction; }
    public Boolean getHasImprovementPlan() { return hasImprovementPlan; }
    public void setHasImprovementPlan(Boolean hasImprovementPlan) { this.hasImprovementPlan = hasImprovementPlan; }
    public Boolean getFirstVisitLate() { return firstVisitLate; }
    public void setFirstVisitLate(Boolean firstVisitLate) { this.firstVisitLate = firstVisitLate; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
