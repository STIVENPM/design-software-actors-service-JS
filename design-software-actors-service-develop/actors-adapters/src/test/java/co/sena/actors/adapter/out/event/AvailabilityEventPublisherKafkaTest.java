package co.sena.actors.adapter.out.event;

import co.sena.actors.core.domain.InstructorAvailabilityException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class AvailabilityEventPublisherKafkaTest {
    @Test
    void writesAvailabilityChangeToOutboxInsteadOfSendingToBroker() {
        var outbox = mock(ActorsOutboxWriter.class);
        var exception = mock(InstructorAvailabilityException.class);
        UUID id = UUID.randomUUID();
        when(exception.getId()).thenReturn(id);
        new AvailabilityEventPublisherAdapter(outbox).publishAvailabilityChangedEvent(exception);
        verify(outbox).enqueue("actors.instructor.availability_changed", id, exception);
    }
}
