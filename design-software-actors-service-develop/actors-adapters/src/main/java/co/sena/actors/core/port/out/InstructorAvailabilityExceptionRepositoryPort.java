package co.sena.actors.core.port.out;

import co.sena.actors.core.domain.InstructorAvailabilityException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstructorAvailabilityExceptionRepositoryPort {

    InstructorAvailabilityException save(
            InstructorAvailabilityException exception
    );

    boolean existsActiveByInstructorId(UUID instructorId);

    List<InstructorAvailabilityException> findActiveByInstructorId(UUID instructorId);

    List<InstructorAvailabilityException> findOverlapping(
            UUID instructorId,
            LocalDateTime from,
            LocalDateTime to
    );

    Optional<InstructorAvailabilityException> findByIdAndInstructorIdAndNotDeleted(
            UUID id,
            UUID instructorId
    );
}
