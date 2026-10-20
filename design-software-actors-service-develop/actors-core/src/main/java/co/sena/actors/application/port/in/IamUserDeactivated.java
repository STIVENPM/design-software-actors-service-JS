package co.sena.actors.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record IamUserDeactivated(UUID eventId, UUID userId) {
    public IamUserDeactivated {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(userId, "userId");
    }
}
