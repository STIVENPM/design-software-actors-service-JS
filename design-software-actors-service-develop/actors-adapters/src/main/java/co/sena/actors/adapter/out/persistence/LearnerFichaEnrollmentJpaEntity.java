package co.sena.actors.adapter.out.persistence;

import co.sena.actors.domain.model.LearnerEnrollmentStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "learner_ficha_enrollment",
        schema = "actors_learners",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_learner_ficha_enrollment_learner_ficha",
                columnNames = {"learner_id", "ficha_id"}))
@Check(constraints = "stage in ('INDUCTION','LECTURE','PRODUCTIVE') and (completion_date is null or completion_date >= enrollment_date)")
public class LearnerFichaEnrollmentJpaEntity {

    @Id
    private UUID id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(
            name = "learner_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_learner_ficha_enrollment_learner"))
    private LearnerJpaEntity learner;

    @Column(name = "ficha_id", nullable = false)
    private UUID fichaId;

    @Column(name = "enrollment_status_id", nullable = false)
    private UUID enrollmentStatusId;

    @ManyToOne
    @JoinColumn(
            name = "enrollment_status_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_learner_ficha_enrollment_status"))
    private StatusJpaEntity enrollmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    private LearnerEnrollmentStage stage;

    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "is_current", nullable = false)
    private boolean isCurrent;

    @Column(name = "transfer_reason")
    private String transferReason;

    protected LearnerFichaEnrollmentJpaEntity() {}

    public LearnerFichaEnrollmentJpaEntity(
            UUID id,
            LearnerJpaEntity learner,
            UUID fichaId,
            UUID enrollmentStatusId,
            LearnerEnrollmentStage stage,
            LocalDate enrollmentDate,
            LocalDate completionDate,
            boolean isCurrent,
            String transferReason) {
        this.id = id;
        this.learner = learner;
        this.fichaId = fichaId;
        this.enrollmentStatusId = enrollmentStatusId;
        this.stage = stage;
        this.enrollmentDate = enrollmentDate;
        this.completionDate = completionDate;
        this.isCurrent = isCurrent;
        this.transferReason = transferReason;
    }

    public UUID getId() {
        return id;
    }

    public LearnerJpaEntity getLearner() {
        return learner;
    }

    public UUID getFichaId() {
        return fichaId;
    }

    public UUID getEnrollmentStatusId() {
        return enrollmentStatusId;
    }

    public void setEnrollmentStatusId(UUID enrollmentStatusId) {
        this.enrollmentStatusId = enrollmentStatusId;
    }

    public LearnerEnrollmentStage getStage() {
        return stage;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDate completionDate) {
        this.completionDate = completionDate;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }

    public String getTransferReason() {
        return transferReason;
    }
}
