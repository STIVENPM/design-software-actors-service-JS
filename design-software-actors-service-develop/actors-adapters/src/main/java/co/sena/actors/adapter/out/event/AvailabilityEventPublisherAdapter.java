package co.sena.actors.adapter.out.event;

import co.sena.actors.core.domain.InstructorAvailabilityException;
import co.sena.actors.core.port.out.AvailabilityEventPublisherPort;
import org.springframework.stereotype.Component;

@Component
public class AvailabilityEventPublisherAdapter implements AvailabilityEventPublisherPort {
    static final String EVENT_TYPE = "actors.instructor.availability_changed";
    private final ActorsOutboxWriter outbox;

    public AvailabilityEventPublisherAdapter(ActorsOutboxWriter outbox) {
        this.outbox = outbox;
    }

    @Override
    public void publishAvailabilityChangedEvent(InstructorAvailabilityException exception) {
        outbox.enqueue(EVENT_TYPE, exception.getId(), exception);
    }
}
