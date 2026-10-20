package co.sena.actors.core.service;

import co.sena.actors.application.port.out.InstructorRepositoryPort;
import co.sena.actors.core.domain.InstructorAvailabilityException;
import co.sena.actors.core.port.out.AvailabilityEventPublisherPort;
import co.sena.actors.core.port.out.InstructorAvailabilityExceptionRepositoryPort;
import co.sena.actors.domain.model.Instructor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InstructorAvailabilityExceptionServiceTest {

    @Test
    void shouldUseCurrentUserAsApprovedByAndAuditorWhenRequestDoesNotProvideOne() {
        InstructorAvailabilityExceptionRepositoryPort repository = mock(InstructorAvailabilityExceptionRepositoryPort.class);
        InstructorRepositoryPort instructorRepository = mock(InstructorRepositoryPort.class);
        AvailabilityEventPublisherPort publisher = mock(AvailabilityEventPublisherPort.class);
        InstructorAvailabilityExceptionService service = new InstructorAvailabilityExceptionService(repository, instructorRepository, publisher);

        UUID instructorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 9, 15, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 15, 18, 0);

        when(instructorRepository.findActiveById(instructorId)).thenReturn(Optional.of(new Instructor()));
        when(repository.save(any(InstructorAvailabilityException.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createException(instructorId, "SICK_LEAVE", start, end, "Desc", null, actorId);

        ArgumentCaptor<InstructorAvailabilityException> captor = ArgumentCaptor.forClass(InstructorAvailabilityException.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getApprovedBy()).isEqualTo(actorId);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(actorId);
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(actorId);
    }

    @Test
    void shouldRejectInvalidRangeWhenFromIsAfterTo() {
        InstructorAvailabilityExceptionRepositoryPort repository = mock(InstructorAvailabilityExceptionRepositoryPort.class);
        InstructorRepositoryPort instructorRepository = mock(InstructorRepositoryPort.class);
        AvailabilityEventPublisherPort publisher = mock(AvailabilityEventPublisherPort.class);
        InstructorAvailabilityExceptionService service = new InstructorAvailabilityExceptionService(repository, instructorRepository, publisher);

        UUID instructorId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 9, 20, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 9, 20, 9, 0);

        assertThatThrownBy(() -> service.getOverlappingExceptions(instructorId, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");
    }
}
