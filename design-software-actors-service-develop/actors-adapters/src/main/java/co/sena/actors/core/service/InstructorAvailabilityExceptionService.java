package co.sena.actors.core.service;

import co.sena.actors.application.port.out.InstructorRepositoryPort;
import co.sena.actors.core.domain.InstructorAvailabilityException;
import co.sena.actors.domain.exception.InstructorAvailabilityExceptionNotFoundException;
import co.sena.actors.domain.exception.InstructorNotFoundException;
import co.sena.actors.core.port.in.InstructorAvailabilityExceptionUseCase;
import co.sena.actors.core.port.out.AvailabilityEventPublisherPort;
import co.sena.actors.core.port.out.InstructorAvailabilityExceptionRepositoryPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class InstructorAvailabilityExceptionService
        implements InstructorAvailabilityExceptionUseCase {

    private final InstructorAvailabilityExceptionRepositoryPort repositoryPort;
    private final InstructorRepositoryPort instructorRepositoryPort;
    private final AvailabilityEventPublisherPort eventPublisherPort;

    public InstructorAvailabilityExceptionService(
            InstructorAvailabilityExceptionRepositoryPort repositoryPort,
            InstructorRepositoryPort instructorRepositoryPort,
            AvailabilityEventPublisherPort eventPublisherPort) {

        this.repositoryPort = repositoryPort;
        this.instructorRepositoryPort = instructorRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public InstructorAvailabilityException createException(
            UUID instructorId,
            String type,
            LocalDateTime start,
            LocalDateTime end,
            String description,
            UUID approvedBy,
            UUID actorId) {

        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "end_datetime must be after start_datetime"
            );
        }

        if (instructorRepositoryPort.findActiveById(instructorId).isEmpty()) {
            throw new InstructorNotFoundException(instructorId);
        }

        UUID currentUserId = actorId != null ? actorId : UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID approvalOwner = approvedBy != null ? approvedBy : currentUserId;
        LocalDateTime now = LocalDateTime.now();

        InstructorAvailabilityException exception =
                new InstructorAvailabilityException(
                        UUID.randomUUID(),
                        instructorId,
                        type,
                        start,
                        end,
                        description,
                        approvalOwner,
                        now,
                        now,
                        true,
                        null,
                        null,
                        currentUserId,
                        currentUserId,
                        0
                );

        InstructorAvailabilityException saved =
                repositoryPort.save(exception);

        eventPublisherPort.publishAvailabilityChangedEvent(saved);

        return saved;
    }

    @Override
    public List<InstructorAvailabilityException> getOverlappingExceptions(
            UUID instructorId,
            LocalDateTime from,
            LocalDateTime to) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        if (from == null && to == null) {
            return repositoryPort.findActiveByInstructorId(instructorId);
        }

        if (from == null && to != null) {
            return repositoryPort.findActiveByInstructorId(instructorId).stream()
                    .filter(e -> e.getStartDatetime() != null && e.getStartDatetime().isBefore(to))
                    .toList();
        }

        if (from != null && to == null) {
            return repositoryPort.findActiveByInstructorId(instructorId).stream()
                    .filter(e -> e.getEndDatetime() != null && e.getEndDatetime().isAfter(from) || e.getEndDatetime() != null && e.getEndDatetime().isEqual(from))
                    .toList();
        }

        return repositoryPort.findOverlapping(instructorId, from, to);
    }

    @Override
    public void deleteException(
            UUID instructorId,
            UUID exceptionId,
            UUID deletedBy) {

        InstructorAvailabilityException exception =
                repositoryPort
                        .findByIdAndInstructorIdAndNotDeleted(
                                exceptionId,
                                instructorId
                        )
                        .orElseThrow(() ->
                                new InstructorAvailabilityExceptionNotFoundException(
                                        exceptionId,
                                        instructorId
                                )
                        );

        LocalDateTime now = LocalDateTime.now();
        exception.setActive(false);
        exception.setUpdatedAt(now);
        exception.setDeletedAt(now);
        exception.setDeletedBy(deletedBy);
        exception.setUpdatedBy(deletedBy);

        InstructorAvailabilityException updated =
                repositoryPort.save(exception);

        eventPublisherPort.publishAvailabilityChangedEvent(updated);
    }
}
