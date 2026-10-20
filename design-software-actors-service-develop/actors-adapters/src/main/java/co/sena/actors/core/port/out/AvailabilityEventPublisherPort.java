package co.sena.actors.core.port.out;

import co.sena.actors.core.domain.InstructorAvailabilityException;

public interface AvailabilityEventPublisherPort {
    void publishAvailabilityChangedEvent(InstructorAvailabilityException exception);
}
