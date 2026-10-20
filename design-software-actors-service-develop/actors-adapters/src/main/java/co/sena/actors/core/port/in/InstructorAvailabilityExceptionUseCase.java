package co.sena.actors.core.port.in;

import co.sena.actors.core.domain.InstructorAvailabilityException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface InstructorAvailabilityExceptionUseCase {

    InstructorAvailabilityException createException(
            UUID instructorId,
            String type,
            LocalDateTime start,
            LocalDateTime end,
            String description,
            UUID approvedBy,
            UUID actorId
    );

    List<InstructorAvailabilityException> getOverlappingExceptions(
            UUID instructorId,
            LocalDateTime from,
            LocalDateTime to
    );

    void deleteException(
            UUID instructorId,
            UUID exceptionId,
            UUID deletedBy
    );
}
