package co.sena.actors.adapter.out.persistence;

import co.sena.actors.application.port.out.InstructorContractRepositoryPort;
import co.sena.actors.domain.model.InstructorContract;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter for instructor contracts.
 */
@Component
@Observed(name = "instructor_contract.persistence")
@SuppressWarnings("null") // Spring Data return values carry no null contract; editor-only hints, javac is clean
public class InstructorContractPersistenceAdapter implements InstructorContractRepositoryPort {

    private final InstructorContractJpaRepository repository;

    public InstructorContractPersistenceAdapter(InstructorContractJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public InstructorContract save(InstructorContract contract) {
        InstructorContractJpaEntity entity = toEntity(contract);
        InstructorContractJpaEntity saved = repository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InstructorContract> findActiveById(UUID id) {
        return repository.findByIdAndActiveTrue(id).map(InstructorContractPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InstructorContract> findById(UUID id) {
        return repository.findById(id).map(InstructorContractPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorContract> findActiveByInstructorId(UUID instructorId) {
        return repository.findByInstructorIdAndActiveTrue(instructorId).stream()
                .map(InstructorContractPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InstructorContract> findCurrentByInstructorId(UUID instructorId) {
        return repository.findByInstructorIdAndActiveTrueAndCurrentTrue(instructorId)
                .map(InstructorContractPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional
    public InstructorContract saveAsCurrent(InstructorContract contract) {
        repository.findByInstructorIdAndActiveTrueAndCurrentTrue(contract.getInstructorId())
                .filter(previous -> !previous.getId().equals(contract.getId()))
                .ifPresent(previous -> {
                    previous.setCurrent(false);
                    // Flush first: Hibernate issues INSERTs before UPDATEs at
                    // flush time, so without this the new row would violate
                    // uq_instructor_contract_current before the old one closes.
                    repository.saveAndFlush(previous);
                });
        InstructorContractJpaEntity saved = repository.saveAndFlush(toEntity(contract));
        return toDomain(saved);
    }

    static InstructorContractJpaEntity toEntity(InstructorContract domain) {
        InstructorContractJpaEntity entity = new InstructorContractJpaEntity();
        entity.setId(domain.getId());
        entity.setInstructorId(domain.getInstructorId());
        entity.setContractType(domain.getContractType() == null ? null : domain.getContractType().name());
        entity.setStartDate(domain.getStartDate());
        entity.setEndDate(domain.getEndDate());
        entity.setTrainingCenterId(domain.getTrainingCenterId());
        entity.setWeeklyHourLimit(domain.getWeeklyHourLimit());
        entity.setCurrent(domain.isCurrent());
        entity.setNotes(domain.getNotes());
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

    static InstructorContract toDomain(InstructorContractJpaEntity entity) {
        InstructorContract domain = new InstructorContract();
        domain.setId(entity.getId());
        domain.setInstructorId(entity.getInstructorId());
        domain.setContractType(entity.getContractType() == null
                ? null : co.sena.actors.domain.model.ContractType.of(entity.getContractType()));
        domain.setStartDate(entity.getStartDate());
        domain.setEndDate(entity.getEndDate());
        domain.setTrainingCenterId(entity.getTrainingCenterId());
        domain.setWeeklyHourLimit(entity.getWeeklyHourLimit());
        domain.setCurrent(entity.isCurrent());
        domain.setNotes(entity.getNotes());
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
