package co.sena.actors.adapter.out.persistence;

import co.sena.actors.application.port.in.QueryLearnerUseCase.BrowseQuery;
import co.sena.actors.application.port.in.QueryLearnerUseCase.PaginatedResult;
import co.sena.actors.adapter.config.CurrentUserProvider;
import co.sena.actors.application.port.out.LearnerRepositoryPort;
import co.sena.actors.domain.model.Learner;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class LearnerPersistenceAdapter implements LearnerRepositoryPort {

    private final LearnerJpaRepository repository;
    private final CurrentUserProvider currentUserProvider;

    public LearnerPersistenceAdapter(LearnerJpaRepository repository, CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public Learner save(Learner learner) {
        UUID actorId = currentUserProvider.currentUserId();
        LocalDateTime now = LocalDateTime.now();
        var entity = repository.findById(learner.id()).orElseGet(() -> new LearnerJpaEntity(
                learner.id(), learner.userId(), learner.documentType(), learner.documentNumber(),
                learner.firstName(), learner.lastName(), learner.email(), learner.phone(),
                learner.birthDate(), learner.isActive(), learner.createdAt(), actorId, actorId, now));
        boolean deactivated = entity.isActive() && !learner.isActive();
        entity.setDocumentType(learner.documentType());
        entity.setDocumentNumber(learner.documentNumber());
        entity.setFirstName(learner.firstName());
        entity.setLastName(learner.lastName());
        entity.setEmail(learner.email());
        entity.setPhone(learner.phone());
        entity.setBirthDate(learner.birthDate());
        entity.setActive(learner.isActive());
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(actorId);
        if (deactivated) {
            entity.setDeletedAt(now);
            entity.setDeletedBy(actorId);
        }
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Learner> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Learner> findByUserId(UUID userId) {
        return repository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public Optional<Learner> findActiveByUserId(UUID userId) {
        return repository.findActiveByUserId(userId)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByDocumentNumber(String documentNumber) {
        return repository.existsByDocumentNumber(documentNumber);
    }

    @Override
    public PaginatedResult<Learner> findPaginated(BrowseQuery query) {
        Specification<LearnerJpaEntity> spec = (root, cq, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (query.documentNumber() != null && !query.documentNumber().isBlank()) {
                predicates.add(cb.equal(root.get("documentNumber"), query.documentNumber()));
            }
            
            if (query.fichaId() != null || query.enrollmentStatusId() != null) {
                jakarta.persistence.criteria.Join<LearnerJpaEntity, LearnerFichaEnrollmentJpaEntity> enrollmentsJoin = root.join("enrollments");
                
                predicates.add(cb.isTrue(enrollmentsJoin.get("isCurrent")));
                
                if (query.fichaId() != null) {
                    predicates.add(cb.equal(enrollmentsJoin.get("fichaId"), query.fichaId()));
                }
                
                if (query.enrollmentStatusId() != null) {
                    predicates.add(cb.equal(enrollmentsJoin.get("enrollmentStatusId"), query.enrollmentStatusId()));
                }
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        var pageable = PageRequest.of(query.page() - 1, query.pageSize());
        var page = repository.findAll(spec, pageable);

        var data = page.getContent().stream().map(this::toDomain).toList();

        return new PaginatedResult<>(
                data,
                (int) page.getTotalElements(),
                page.getTotalPages(),
                query.page());
    }

    private Learner toDomain(LearnerJpaEntity entity) {
        return new Learner(
                entity.getId(),
                entity.getUserId(),
                entity.getDocumentType(),
                entity.getDocumentNumber(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getBirthDate(),
                entity.isActive(),
                entity.getCreatedAt());
    }
}
