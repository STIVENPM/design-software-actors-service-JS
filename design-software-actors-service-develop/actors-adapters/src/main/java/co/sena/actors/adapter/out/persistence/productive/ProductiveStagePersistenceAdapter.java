package co.sena.actors.adapter.out.persistence.productive;

import co.sena.actors.application.port.in.ProductiveStageUseCase;
import co.sena.actors.application.port.out.ProductiveStageRepositoryPort;
import co.sena.actors.domain.model.CompanyVisit;
import co.sena.actors.domain.model.ProductiveStage;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ProductiveStagePersistenceAdapter implements ProductiveStageRepositoryPort {
    private final ProductiveStageJpaRepository stages;
    private final CompanyVisitJpaRepository visits;
    private final JdbcTemplate jdbc;

    public ProductiveStagePersistenceAdapter(ProductiveStageJpaRepository stages,
            CompanyVisitJpaRepository visits, JdbcTemplate jdbc) {
        this.stages = stages;
        this.visits = visits;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public ProductiveStage save(ProductiveStage stage) {
        var entity = stages.findById(stage.id()).orElseGet(ProductiveStageJpaEntity::new);
        if (entity.getId() != null && entity.getRowVersion() != stage.rowVersion()) {
            throw new ObjectOptimisticLockingFailureException(ProductiveStageJpaEntity.class, stage.id());
        }
        entity.setId(stage.id());
        entity.setLearnerId(stage.learnerId());
        entity.setCompanyId(stage.companyId());
        entity.setSupervisorInstructorId(stage.supervisorInstructorId());
        entity.setStatusId(stage.statusId());
        entity.setStartDate(stage.startDate());
        entity.setPlannedEndDate(stage.plannedEndDate());
        entity.setTotalHoursRequired(stage.totalHoursRequired());
        entity.setTotalHoursCompleted(stage.totalHoursCompleted());
        entity.setInterruptionReason(stage.interruptionReason());
        entity.setCreatedAt(stage.createdAt());
        entity.setCreatedBy(stage.createdBy());
        entity.setUpdatedAt(stage.updatedAt());
        entity.setUpdatedBy(stage.updatedBy());
        return toDomain(stages.saveAndFlush(entity));
    }

    @Override
    public Optional<ProductiveStage> findById(UUID id) {
        return stages.findById(id).map(this::toDomain);
    }

    @Override
    public ProductiveStageUseCase.Page findAll(ProductiveStageUseCase.Query query) {
        var page = stages.findAll((root, criteria, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (query.learnerId() != null) {
                predicates.add(cb.equal(root.get("learnerId"), query.learnerId()));
            }
            if (query.companyId() != null) {
                predicates.add(cb.equal(root.get("companyId"), query.companyId()));
            }
            if (query.statusId() != null) {
                predicates.add(cb.equal(root.get("statusId"), query.statusId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(query.page() - 1, query.pageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"))));
        return new ProductiveStageUseCase.Page(page.getContent().stream().map(this::toDomain).toList(),
                query.page(), query.pageSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional
    public CompanyVisit saveVisit(CompanyVisit visit) {
        var entity = new CompanyVisitJpaEntity();
        entity.setId(visit.id());
        entity.setProductiveStageId(visit.productiveStageId());
        entity.setInstructorId(visit.instructorId());
        entity.setVisitNumber(visit.visitNumber());
        entity.setVisitDate(visit.visitDate());
        entity.setVisitType(visit.visitType());
        entity.setLearnerPerformance(visit.learnerPerformance());
        entity.setCompanySatisfaction(visit.companySatisfaction());
        entity.setHasImprovementPlan(visit.hasImprovementPlan());
        entity.setFirstVisitLate(visit.firstVisitLate());
        entity.setCreatedAt(visit.createdAt());
        entity.setCreatedBy(visit.createdBy());
        return toDomain(visits.saveAndFlush(entity));
    }

    @Override
    public long countVisits(UUID productiveStageId) {
        return visits.countByProductiveStageId(productiveStageId);
    }

    @Override
    public boolean existsLearner(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM actors_learners.learner WHERE id = ?)",
                Boolean.class, id));
    }

    @Override
    public boolean existsActiveCompany(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM actors_companies.company WHERE id = ? AND is_active = true)",
                Boolean.class, id));
    }

    @Override
    public boolean existsActiveInstructor(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM actors_instructors.instructor WHERE id = ? AND is_active = true)",
                Boolean.class, id));
    }

    @Override
    public Optional<StatusInfo> statusInfo(UUID id) {
        var rows = jdbc.query("""
                SELECT s.code, COALESCE(s.is_terminal, false) AS terminal
                FROM actors_parameterization.status s
                JOIN actors_parameterization.status_category c ON s.status_category_id = c.id
                WHERE s.id = ? AND c.code = 'PRODUCTIVE_STAGE'
                """, (rs, rowNum) -> new StatusInfo(id, rs.getString("code"), rs.getBoolean("terminal")), id);
        return rows.stream().findFirst();
    }

    private ProductiveStage toDomain(ProductiveStageJpaEntity entity) {
        return new ProductiveStage(entity.getId(), entity.getLearnerId(), entity.getCompanyId(),
                entity.getSupervisorInstructorId(), entity.getStatusId(), entity.getStartDate(),
                entity.getPlannedEndDate(), entity.getTotalHoursRequired(), entity.getTotalHoursCompleted(),
                entity.getInterruptionReason(), entity.getCreatedAt(), entity.getCreatedBy(),
                entity.getUpdatedAt(), entity.getUpdatedBy(), entity.getRowVersion());
    }

    private CompanyVisit toDomain(CompanyVisitJpaEntity entity) {
        return new CompanyVisit(entity.getId(), entity.getProductiveStageId(), entity.getInstructorId(),
                entity.getVisitNumber(), entity.getVisitDate(), entity.getVisitType(),
                entity.getLearnerPerformance(), entity.getCompanySatisfaction(),
                entity.getHasImprovementPlan(), entity.getFirstVisitLate(),
                entity.getCreatedAt(), entity.getCreatedBy());
    }
}
