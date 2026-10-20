package co.sena.actors.adapter.out.persistence;

import co.sena.actors.application.port.out.ActivityLogRepositoryPort;
import co.sena.actors.domain.model.ActivityLogEntry;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import io.micrometer.observation.annotation.Observed;
import java.util.stream.Collectors;

@Component
@Observed(name = "activity.log.repository")
public class ActivityLogPersistenceAdapter implements ActivityLogRepositoryPort {

    private final ActivityLogJpaRepository repository;

    public ActivityLogPersistenceAdapter(ActivityLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(ActivityLogEntry entry) {
        repository.save(toEntity(entry));
    }

    @Override
    public List<ActivityLogEntry> findByInstructorIdWithCursor(UUID instructorId, String cursor, int limit) {
        List<ActivityLogJpaEntity> entities;
        if (cursor == null) {
            entities = repository.findByInstructorIdFirstPage(instructorId, PageRequest.of(0, limit));
        } else {
            try {
                entities = repository.findByInstructorIdWithCursor(instructorId, OffsetDateTime.parse(cursor), PageRequest.of(0, limit));
            } catch (java.time.format.DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid cursor format");
            }
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ActivityLogEntry> findByLearnerIdWithCursor(UUID learnerId, String cursor, int limit) {
        List<ActivityLogJpaEntity> entities;
        if (cursor == null) {
            entities = repository.findByLearnerIdFirstPage(learnerId, PageRequest.of(0, limit));
        } else {
            try {
                entities = repository.findByLearnerIdWithCursor(learnerId, OffsetDateTime.parse(cursor), PageRequest.of(0, limit));
            } catch (java.time.format.DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid cursor format");
            }
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    private ActivityLogJpaEntity toEntity(ActivityLogEntry domain) {
        ActivityLogJpaEntity entity = new ActivityLogJpaEntity();
        entity.setId(domain.getId());
        entity.setInstructorId(domain.getInstructorId());
        entity.setLearnerId(domain.getLearnerId());
        entity.setEventType(domain.getEventType());
        entity.setDescription(domain.getDescription());
        entity.setPreviousValue(domain.getPreviousValue());
        entity.setNewValue(domain.getNewValue());
        entity.setRecordedBy(domain.getRecordedBy());
        entity.setRecordedAt(domain.getRecordedAt());
        return entity;
    }

    private ActivityLogEntry toDomain(ActivityLogJpaEntity entity) {
        return new ActivityLogEntry(
                entity.getId(),
                entity.getInstructorId(),
                entity.getLearnerId(),
                entity.getEventType(),
                entity.getDescription(),
                entity.getPreviousValue(),
                entity.getNewValue(),
                entity.getRecordedBy(),
                entity.getRecordedAt()
        );
    }
}
