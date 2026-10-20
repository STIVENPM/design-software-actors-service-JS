package co.sena.actors.application.port.out;

import java.util.UUID;

/** Must share the transaction of the actor changes and activity log. */
public interface IamEventInboxPort {
    /** @return false for a previously processed event_id. */
    boolean claim(UUID eventId, UUID userId, String eventType);
}
