package co.sena.actors.adapter.out.persistence;

import co.sena.actors.application.port.out.InstructorAreaRepositoryPort;
import co.sena.actors.domain.model.InstructorArea;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter for instructor areas.
 */
@Component
@Observed(name = "instructor_area.persistence")
@SuppressWarnings("null") // Spring Data return values carry no null contract; editor-only hints, javac is clean
public class InstructorAreaPersistenceAdapter implements InstructorAreaRepositoryPort {

    private final InstructorAreaJpaRepository repository;

    public InstructorAreaPersistenceAdapter(InstructorAreaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public InstructorArea save(InstructorArea area) {
        InstructorAreaJpaEntity entity = toEntity(area);
        InstructorAreaJpaEntity saved = repository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InstructorArea> findActiveById(UUID id) {
        return repository.findByIdAndActiveTrue(id).map(InstructorAreaPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InstructorArea> findById(UUID id) {
        return repository.findById(id).map(InstructorAreaPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorArea> findActiveByInstructorId(UUID instructorId) {
        return repository.findByInstructorIdAndActiveTrue(instructorId).stream()
                .map(InstructorAreaPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByInstructorIdAndAreaName(UUID instructorId, String areaName) {
        return repository.existsByInstructorIdAndAreaName(instructorId, areaName);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByInstructorIdAndAreaNameExcludingId(UUID instructorId, String areaName, UUID id) {
        return repository.existsByInstructorIdAndAreaNameAndIdNot(instructorId, areaName, id);
    }

    static InstructorAreaJpaEntity toEntity(InstructorArea domain) {
        InstructorAreaJpaEntity entity = new InstructorAreaJpaEntity();
        entity.setId(domain.getId());
        entity.setInstructorId(domain.getInstructorId());
        entity.setAreaName(domain.getAreaName());
        entity.setPrimary(domain.isPrimary());
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

    static InstructorArea toDomain(InstructorAreaJpaEntity entity) {
        InstructorArea domain = new InstructorArea();
        domain.setId(entity.getId());
        domain.setInstructorId(entity.getInstructorId());
        domain.setAreaName(entity.getAreaName());
        domain.setPrimary(entity.isPrimary());
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
