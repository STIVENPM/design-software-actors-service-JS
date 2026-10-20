package co.sena.actors.adapter.out.persistence;

import co.sena.actors.application.port.out.InstructorRepositoryPort;
import co.sena.actors.domain.model.DocumentType;
import co.sena.actors.domain.model.Instructor;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter. It never calls delete(): deactivation is logical (E4).
 */
@Component
@Observed(name = "instructor.persistence")
@SuppressWarnings("null") // Spring Data return values carry no null contract; editor-only hints, javac is clean
public class InstructorPersistenceAdapter implements InstructorRepositoryPort {

    private final InstructorJpaRepository repository;

    public InstructorPersistenceAdapter(InstructorJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Instructor save(Instructor instructor) {
        InstructorJpaEntity entity = toEntity(instructor);
        // saveAndFlush: materializes the @Version increment before mapping the response
        InstructorJpaEntity saved = repository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instructor> findActiveById(UUID id) {
        return repository.findByIdAndActiveTrue(id).map(InstructorPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instructor> findById(UUID id) {
        return repository.findById(id).map(InstructorPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instructor> findByUserId(UUID userId) {
        return repository.findByUserId(userId).map(InstructorPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActive(boolean active, String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            return repository.countByActive(active);
        }
        return repository.countByActiveAndDocumentNumberContainingIgnoreCase(active, documentNumber.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Instructor> findActiveSlice(
            boolean active, String documentNumber, String sortProperty, boolean ascending, int offset, int limit) {
        org.springframework.data.domain.Pageable pageable = pageRequest(sortProperty, ascending, offset, limit);
        String doc = normalize(documentNumber);
        org.springframework.data.domain.Page<InstructorJpaEntity> page = (doc == null)
                ? repository.findAllByActive(active, pageable)
                : repository.findAllByActiveAndDocumentNumberContainingIgnoreCase(active, doc, pageable);
        return page.getContent().stream().map(InstructorPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveInCenter(boolean active, String documentNumber, UUID trainingCenterId) {
        String doc = normalize(documentNumber);
        return doc == null
                ? repository.countByActiveAndCurrentCenter(active, trainingCenterId)
                : repository.countByActiveAndDocumentNumberAndCurrentCenter(active, doc, trainingCenterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Instructor> findActiveSliceInCenter(
            boolean active, String documentNumber, String sortProperty, boolean ascending, int offset, int limit,
            UUID trainingCenterId) {
        org.springframework.data.domain.Pageable pageable = pageRequest(sortProperty, ascending, offset, limit);
        String doc = normalize(documentNumber);
        org.springframework.data.domain.Page<InstructorJpaEntity> page = doc == null
                ? repository.findAllByActiveAndCurrentCenter(active, trainingCenterId, pageable)
                : repository.findAllByActiveAndDocumentNumberAndCurrentCenter(active, doc, trainingCenterId, pageable);
        return page.getContent().stream().map(InstructorPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findCurrentTrainingCenterId(UUID instructorId) {
        return repository.findCurrentTrainingCenterId(instructorId);
    }

    private static org.springframework.data.domain.Pageable pageRequest(
            String sortProperty, boolean ascending, int offset, int limit) {
        int size = Math.max(limit, 1);
        return org.springframework.data.domain.PageRequest.of(
                Math.max(offset, 0) / size, size,
                ascending ? org.springframework.data.domain.Sort.by(sortProperty).ascending()
                        : org.springframework.data.domain.Sort.by(sortProperty).descending());
    }

    private static String normalize(String documentNumber) {
        return (documentNumber == null || documentNumber.isBlank()) ? null : documentNumber.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByDocumentNumber(String documentNumber) {
        return repository.existsByDocumentNumber(documentNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByDocumentNumberExcludingId(String documentNumber, UUID id) {
        return repository.existsByDocumentNumberAndIdNot(documentNumber, id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserId(UUID userId) {
        return repository.existsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdExcludingId(UUID userId, UUID id) {
        return repository.existsByUserIdAndIdNot(userId, id);
    }

    static InstructorJpaEntity toEntity(Instructor domain) {
        InstructorJpaEntity entity = new InstructorJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setDocumentType(domain.getDocumentType() == null ? null : domain.getDocumentType().name());
        entity.setDocumentNumber(domain.getDocumentNumber());
        entity.setFirstName(domain.getFirstName());
        entity.setLastName(domain.getLastName());
        entity.setEmail(domain.getEmail());
        entity.setPhone(domain.getPhone());
        entity.setDefaultMaxHoursPerWeek(domain.getDefaultMaxHoursPerWeek());
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

    static Instructor toDomain(InstructorJpaEntity entity) {
        Instructor domain = new Instructor();
        domain.setId(entity.getId());
        domain.setUserId(entity.getUserId());
        domain.setDocumentType(entity.getDocumentType() == null
                ? null : DocumentType.of(entity.getDocumentType()));
        domain.setDocumentNumber(entity.getDocumentNumber());
        domain.setFirstName(entity.getFirstName());
        domain.setLastName(entity.getLastName());
        domain.setEmail(entity.getEmail());
        domain.setPhone(entity.getPhone());
        domain.setDefaultMaxHoursPerWeek(entity.getDefaultMaxHoursPerWeek());
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
