package co.sena.actors.adapter.out.persistence;

import co.sena.actors.application.port.out.CompetencyAssignmentRepositoryPort;
import co.sena.actors.domain.model.CompetencyAssignment;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter for competency assignments. It never calls delete():
 * revocation is logical (E2).
 */
@Component
@Observed(name = "competency_assignment.persistence")
@SuppressWarnings("null") // Spring Data return values carry no null contract; editor-only hints, javac is clean
public class CompetencyAssignmentPersistenceAdapter implements CompetencyAssignmentRepositoryPort {

    private final CompetencyAssignmentJpaRepository repository;

    public CompetencyAssignmentPersistenceAdapter(CompetencyAssignmentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CompetencyAssignment save(CompetencyAssignment assignment) {
        CompetencyAssignmentJpaEntity entity = toEntity(assignment);
        CompetencyAssignmentJpaEntity saved = repository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompetencyAssignment> findActiveById(UUID id) {
        return repository.findByIdAndActiveTrue(id).map(CompetencyAssignmentPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompetencyAssignment> findById(UUID id) {
        return repository.findById(id).map(CompetencyAssignmentPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompetencyAssignment> findActiveByInstructorId(UUID instructorId) {
        return repository.findByInstructorIdAndActiveTrue(instructorId).stream()
                .map(CompetencyAssignmentPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveByInstructorIdAndCompetencyId(UUID instructorId, UUID competencyId) {
        return repository.existsByInstructorIdAndCompetencyIdAndActiveTrue(instructorId, competencyId);
    }

    static CompetencyAssignmentJpaEntity toEntity(CompetencyAssignment domain) {
        CompetencyAssignmentJpaEntity entity = new CompetencyAssignmentJpaEntity();
        entity.setId(domain.getId());
        entity.setInstructorId(domain.getInstructorId());
        entity.setCompetencyId(domain.getCompetencyId());
        entity.setCertifiedAt(domain.getCertifiedAt());
        entity.setCertificationEntity(domain.getCertificationEntity());
        // requires_review exists in DDL but is owned by HU-ACT-013
        // (academic.competency.updated consumption); always false here.
        entity.setRequiresReview(false);
        entity.setActive(domain.isActive());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setUpdatedBy(domain.getUpdatedBy());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setDeletedBy(domain.getDeletedBy());
        entity.setRowVersion(domain.getRowVersion());
        return entity;
    }

    static CompetencyAssignment toDomain(CompetencyAssignmentJpaEntity entity) {
        CompetencyAssignment domain = new CompetencyAssignment();
        domain.setId(entity.getId());
        domain.setInstructorId(entity.getInstructorId());
        domain.setCompetencyId(entity.getCompetencyId());
        domain.setCertifiedAt(entity.getCertifiedAt());
        domain.setCertificationEntity(entity.getCertificationEntity());
        domain.setActive(entity.isActive());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setCreatedBy(entity.getCreatedBy());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setUpdatedBy(entity.getUpdatedBy());
        domain.setDeletedAt(entity.getDeletedAt());
        domain.setDeletedBy(entity.getDeletedBy());
        domain.setRowVersion(entity.getRowVersion());
        return domain;
    }
}
