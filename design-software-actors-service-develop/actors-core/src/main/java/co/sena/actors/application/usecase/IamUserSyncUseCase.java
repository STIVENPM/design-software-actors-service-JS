package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.IamActorType;
import co.sena.actors.application.port.in.IamUserCreated;
import co.sena.actors.application.port.in.IamUserDeactivated;
import co.sena.actors.application.port.out.ActivityLogRepositoryPort;
import co.sena.actors.application.port.out.CompetencyAssignmentRepositoryPort;
import co.sena.actors.application.port.out.IamEventInboxPort;
import co.sena.actors.application.port.out.InstructorAreaRepositoryPort;
import co.sena.actors.application.port.out.InstructorRepositoryPort;
import co.sena.actors.application.port.out.LearnerRepositoryPort;
import co.sena.actors.domain.model.ActivityLogEntry;
import co.sena.actors.domain.model.DocumentType;
import co.sena.actors.domain.model.Instructor;
import co.sena.actors.domain.model.Learner;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring-free event decisions. The caller must provide one DB transaction covering the
 * inbox claim, every profile change and the informational activity log.
 */
public final class IamUserSyncUseCase {

    public enum Result { PROCESSED, DUPLICATE }

    private final IamEventInboxPort inbox;
    private final InstructorRepositoryPort instructors;
    private final LearnerRepositoryPort learners;
    private final CompetencyAssignmentRepositoryPort assignments;
    private final InstructorAreaRepositoryPort areas;
    private final ActivityLogRepositoryPort activityLog;
    private final Clock clock;
    private final UUID systemActorId;

    public IamUserSyncUseCase(IamEventInboxPort inbox, InstructorRepositoryPort instructors,
            LearnerRepositoryPort learners, CompetencyAssignmentRepositoryPort assignments,
            InstructorAreaRepositoryPort areas, ActivityLogRepositoryPort activityLog,
            Clock clock, UUID systemActorId) {
        this.inbox = Objects.requireNonNull(inbox);
        this.instructors = Objects.requireNonNull(instructors);
        this.learners = Objects.requireNonNull(learners);
        this.assignments = Objects.requireNonNull(assignments);
        this.areas = Objects.requireNonNull(areas);
        this.activityLog = Objects.requireNonNull(activityLog);
        this.clock = Objects.requireNonNull(clock);
        this.systemActorId = Objects.requireNonNull(systemActorId);
    }

    public Result created(IamUserCreated event) {
        Objects.requireNonNull(event);
        if (!inbox.claim(event.eventId(), event.userId(), "iam.user.created")) {
            return Result.DUPLICATE;
        }
        if (event.actorType() == IamActorType.USER) {
            return Result.PROCESSED;
        }
        Optional<Instructor> instructor = instructors.findByUserId(event.userId());
        Optional<Learner> learner = learners.findByUserId(event.userId());
        rejectAmbiguousProfile(event.userId(), instructor, learner);
        OffsetDateTime now = now();
        if (event.actorType() == IamActorType.INSTRUCTOR && instructor.isPresent()) {
            Instructor profile = instructor.get();
            requireActive(profile.isActive(), event.userId());
            profile.setFirstName(event.firstName().trim());
            profile.setLastName(event.lastName().trim());
            profile.setEmail(event.email().trim());
            profile.setUpdatedAt(now);
            profile.setUpdatedBy(systemActorId);
            instructors.save(profile);
            log(profile.getId(), null, "iam.user.created", "IAM instructor identity synchronized", null, event.userId().toString());
            return Result.PROCESSED;
        }
        if (event.actorType() == IamActorType.INSTRUCTOR) {
            Instructor profile = new Instructor();
            profile.setId(UUID.randomUUID());
            profile.setUserId(event.userId());
            profile.setDocumentType(documentType(event));
            profile.setDocumentNumber(required(event.documentNumber(), "document_number"));
            profile.setFirstName(event.firstName().trim());
            profile.setLastName(event.lastName().trim());
            profile.setEmail(event.email().trim());
            profile.setDefaultMaxHoursPerWeek(defaultHours(event));
            profile.setActive(true);
            profile.setCreatedAt(now);
            profile.setCreatedBy(systemActorId);
            profile.setUpdatedAt(now);
            profile.setUpdatedBy(systemActorId);
            instructors.save(profile);
            log(profile.getId(), null, "iam.user.created", "IAM instructor provisioned", null, event.userId().toString());
            return Result.PROCESSED;
        }
        if (event.actorType() == IamActorType.LEARNER && learner.isPresent()) {
            Learner profile = learner.get();
            requireActive(profile.isActive(), event.userId());
            learners.save(profile.update(profile.documentType(), profile.documentNumber(),
                    event.firstName().trim(), event.lastName().trim(), event.email().trim(),
                    profile.phone(), profile.birthDate()));
            log(null, profile.id(), "iam.user.created", "IAM learner identity synchronized", null, event.userId().toString());
            return Result.PROCESSED;
        }
        if (event.actorType() == IamActorType.LEARNER) {
            Learner profile = Learner.create(UUID.randomUUID(), event.userId(),
                    documentType(event), required(event.documentNumber(), "document_number"),
                    event.firstName().trim(), event.lastName().trim(), event.email().trim(),
                    null, null);
            learners.save(profile);
            log(null, profile.id(), "iam.user.created", "IAM learner provisioned", null, event.userId().toString());
            return Result.PROCESSED;
        }
        throw new UnprovisionedIamActorException(event.userId());
    }

    public Result deactivated(IamUserDeactivated event) {
        Objects.requireNonNull(event);
        if (!inbox.claim(event.eventId(), event.userId(), "iam.user.deactivated")) {
            return Result.DUPLICATE;
        }
        Optional<Instructor> instructor = instructors.findByUserId(event.userId());
        Optional<Learner> learner = learners.findByUserId(event.userId());
        rejectAmbiguousProfile(event.userId(), instructor, learner);
        OffsetDateTime now = now();
        if (instructor.isPresent()) {
            Instructor profile = instructor.get();
            if (!profile.isActive()) {
                return Result.PROCESSED;
            }
            profile.setActive(false);
            profile.setDeletedAt(now);
            profile.setDeletedBy(systemActorId);
            profile.setUpdatedAt(now);
            profile.setUpdatedBy(systemActorId);
            instructors.save(profile);
            assignments.findActiveByInstructorId(profile.getId()).forEach(assignment -> {
                assignment.setActive(false);
                assignment.setDeletedAt(now);
                assignment.setDeletedBy(systemActorId);
                assignment.setUpdatedAt(now);
                assignment.setUpdatedBy(systemActorId);
                assignments.save(assignment);
            });
            areas.findActiveByInstructorId(profile.getId()).forEach(area -> {
                area.setActive(false);
                area.setDeletedAt(now);
                area.setDeletedBy(systemActorId);
                area.setUpdatedAt(now);
                area.setUpdatedBy(systemActorId);
                areas.save(area);
            });
            log(profile.getId(), null, "iam.user.deactivated", "IAM instructor deactivated", "true", "false");
            return Result.PROCESSED;
        }
        if (learner.isPresent()) {
            Learner profile = learner.get();
            if (!profile.isActive()) {
                return Result.PROCESSED;
            }
            learners.save(profile.deactivate());
            // Enrollment is deliberately untouched: no rule has been approved for it.
            log(null, profile.id(), "iam.user.deactivated", "IAM learner deactivated", "true", "false");
            return Result.PROCESSED;
        }
        // A deactivation before creation cannot be safely acknowledged without a durable
        // ordering decision/tombstone. Let the caller roll back and route it for inspection.
        throw new UnprovisionedIamActorException(event.userId());
    }

    private void rejectAmbiguousProfile(UUID userId, Optional<Instructor> instructor, Optional<Learner> learner) {
        if (instructor.isPresent() && learner.isPresent()) {
            throw new IllegalStateException("IAM user_id has both actor profiles: " + userId);
        }
    }

    private void requireActive(boolean active, UUID userId) {
        if (!active) {
            throw new IllegalStateException("IAM creation arrived for inactive actor: " + userId);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private DocumentType documentType(IamUserCreated event) {
        return DocumentType.of(required(event.documentType(), "document_type"));
    }

    private BigDecimal defaultHours(IamUserCreated event) {
        if (event.defaultMaxHoursPerWeek() == null) {
            throw new IllegalArgumentException("IAM actor event requires default_max_hours_per_week");
        }
        if (event.defaultMaxHoursPerWeek().signum() < 0) {
            throw new IllegalArgumentException("default_max_hours_per_week must be >= 0");
        }
        return event.defaultMaxHoursPerWeek();
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("IAM actor event requires " + field);
        }
        return value.trim();
    }

    private void log(UUID instructorId, UUID learnerId, String type, String description,
            String previous, String next) {
        activityLog.save(new ActivityLogEntry(UUID.randomUUID(), instructorId, learnerId,
                type, description, previous, next, systemActorId, now()));
    }
}
