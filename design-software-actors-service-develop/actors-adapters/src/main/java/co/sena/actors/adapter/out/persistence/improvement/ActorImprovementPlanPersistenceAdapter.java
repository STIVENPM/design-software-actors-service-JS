package co.sena.actors.adapter.out.persistence.improvement;

import co.sena.actors.application.port.in.ManageImprovementPlanUseCase;
import co.sena.actors.application.port.out.ImprovementPlanRepositoryPort;
import co.sena.actors.domain.model.ActorImprovementPlan;
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
public class ActorImprovementPlanPersistenceAdapter implements ImprovementPlanRepositoryPort {
    private final ActorImprovementPlanJpaRepository plans;
    private final JdbcTemplate jdbc;

    public ActorImprovementPlanPersistenceAdapter(ActorImprovementPlanJpaRepository plans, JdbcTemplate jdbc) {
        this.plans = plans;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public ActorImprovementPlan save(ActorImprovementPlan plan) {
        var entity = plans.findById(plan.id()).orElseGet(ActorImprovementPlanJpaEntity::new);
        if (entity.getId() != null && entity.getRowVersion() != plan.rowVersion()) {
            throw new ObjectOptimisticLockingFailureException(ActorImprovementPlanJpaEntity.class, plan.id());
        }
        entity.setId(plan.id());
        entity.setInstructorId(plan.instructorId());
        entity.setLearnerId(plan.learnerId());
        entity.setOrigin(plan.origin());
        entity.setOriginReferenceId(plan.originReferenceId());
        entity.setDescription(plan.description());
        entity.setSpecificGoals(plan.specificGoals());
        entity.setDueDate(plan.dueDate());
        entity.setStatusId(plan.statusId());
        entity.setCreatedBy(plan.createdBy());
        entity.setCreatedAt(plan.createdAt());
        entity.setCompletedAt(plan.completedAt());
        entity.setUpdatedAt(plan.updatedAt());
        entity.setUpdatedBy(plan.updatedBy());
        entity.setIsActive(plan.isActive());
        entity.setDeletedAt(plan.deletedAt());
        entity.setDeletedBy(plan.deletedBy());
        return toDomain(plans.saveAndFlush(entity));
    }

    @Override
    public Optional<ActorImprovementPlan> findById(UUID id) {
        return plans.findById(id)
                .filter(plan -> Boolean.TRUE.equals(plan.getIsActive()) && plan.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public ManageImprovementPlanUseCase.Page findAll(ManageImprovementPlanUseCase.Query query) {
        var page = plans.findAll((root, criteria, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("isActive"), true));
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (query.instructorId() != null) predicates.add(cb.equal(root.get("instructorId"), query.instructorId()));
            if (query.learnerId() != null) predicates.add(cb.equal(root.get("learnerId"), query.learnerId()));
            if (query.statusId() != null) predicates.add(cb.equal(root.get("statusId"), query.statusId()));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(query.page() - 1, query.pageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"))));
        return new ManageImprovementPlanUseCase.Page(page.getContent().stream().map(this::toDomain).toList(),
                query.page(), query.pageSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public Optional<StatusInfo> statusInfo(UUID id) {
        var rows = jdbc.query("""
                        SELECT s.id, COALESCE(s.is_terminal, false) AS terminal
                        FROM actors_parameterization.status s
                        JOIN actors_parameterization.status_category c ON s.status_category_id = c.id
                        WHERE s.id = ? AND c.code = 'IMPROVEMENT_PLAN'
                        """,
                (rs, rowNum) -> new StatusInfo(rs.getObject("id", UUID.class), rs.getBoolean("terminal")), id);
        return rows.stream().findFirst();
    }

    private ActorImprovementPlan toDomain(ActorImprovementPlanJpaEntity entity) {
        return new ActorImprovementPlan(entity.getId(), entity.getInstructorId(), entity.getLearnerId(),
                entity.getOrigin(), entity.getOriginReferenceId(), entity.getDescription(), entity.getSpecificGoals(),
                entity.getDueDate(), entity.getStatusId(), entity.getCreatedBy(), entity.getCreatedAt(),
                entity.getCompletedAt(), entity.getUpdatedAt(), entity.getUpdatedBy(), entity.getRowVersion(),
                entity.getIsActive(), entity.getDeletedAt(), entity.getDeletedBy());
    }
}