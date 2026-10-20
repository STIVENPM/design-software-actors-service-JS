package co.sena.actors.adapter.out.persistence;

import co.sena.actors.core.domain.InstructorAvailabilityException;
import co.sena.actors.core.port.out.InstructorAvailabilityExceptionRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class InstructorAvailabilityExceptionPersistenceAdapter
        implements InstructorAvailabilityExceptionRepositoryPort {

    private final InstructorAvailabilityExceptionJpaRepository jpaRepository;

    public InstructorAvailabilityExceptionPersistenceAdapter(
            InstructorAvailabilityExceptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public InstructorAvailabilityException save(
            InstructorAvailabilityException exception) {

        InstructorAvailabilityExceptionJpaEntity entity = toEntity(exception);

        InstructorAvailabilityExceptionJpaEntity saved =
                jpaRepository.save(entity);

        return toDomain(saved);
    }

    @Override
    public boolean existsActiveByInstructorId(UUID instructorId) {
        return jpaRepository.existsByInstructorIdAndActiveTrue(instructorId);
    }

    @Override
    public List<InstructorAvailabilityException> findActiveByInstructorId(UUID instructorId) {
        return jpaRepository.findByInstructorIdAndActiveTrueOrderByStartDatetimeAsc(instructorId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InstructorAvailabilityException> findOverlapping(
            UUID instructorId,
            LocalDateTime from,
            LocalDateTime to) {

        return jpaRepository.findOverlapping(
                        instructorId,
                        from,
                        to)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<InstructorAvailabilityException>
    findByIdAndInstructorIdAndNotDeleted(
            UUID id,
            UUID instructorId) {

        return jpaRepository.findActiveByIdAndInstructorId(
                        id,
                        instructorId)
                .map(this::toDomain);
    }

    private InstructorAvailabilityExceptionJpaEntity toEntity(
            InstructorAvailabilityException domain) {

        InstructorAvailabilityExceptionJpaEntity entity =
                new InstructorAvailabilityExceptionJpaEntity();

        entity.setId(domain.getId() != null ? domain.getId() : UUID.randomUUID());
        entity.setInstructorId(domain.getInstructorId());
        entity.setExceptionType(domain.getExceptionType());
        entity.setStartDatetime(domain.getStartDatetime());
        entity.setEndDatetime(domain.getEndDatetime());
        entity.setDescription(domain.getDescription());
        entity.setApprovedBy(domain.getApprovedBy());
        entity.setActive(domain.isActive());
        entity.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now());
        entity.setUpdatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : LocalDateTime.now());
        entity.setCreatedBy(domain.getCreatedBy() != null ? domain.getCreatedBy() : UUID.randomUUID());
        entity.setUpdatedBy(domain.getUpdatedBy() != null ? domain.getUpdatedBy() : domain.getCreatedBy() != null ? domain.getCreatedBy() : UUID.randomUUID());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setDeletedBy(domain.getDeletedBy());
        entity.setRowVersion(domain.getRowVersion());

        return entity;
    }

    private InstructorAvailabilityException toDomain(
            InstructorAvailabilityExceptionJpaEntity entity) {

        return new InstructorAvailabilityException(
                entity.getId(),
                entity.getInstructorId(),
                entity.getExceptionType(),
                entity.getStartDatetime(),
                entity.getEndDatetime(),
                entity.getDescription(),
                entity.getApprovedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.isActive(),
                entity.getDeletedAt(),
                entity.getDeletedBy(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getRowVersion()
        );
    }
}
