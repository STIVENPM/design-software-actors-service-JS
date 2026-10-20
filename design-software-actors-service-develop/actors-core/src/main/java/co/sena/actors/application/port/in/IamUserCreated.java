package co.sena.actors.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Identity facts from IAM. Roles are accepted but are owned by IAM, not actors. */
public record IamUserCreated(UUID eventId, UUID userId, IamActorType actorType,
        String email, String firstName, String lastName, String documentType,
        String documentNumber, BigDecimal defaultMaxHoursPerWeek, List<String> roles) {

    public IamUserCreated(UUID eventId, UUID userId, IamActorType actorType,
            String email, String firstName, String lastName, List<String> roles) {
        this(eventId, userId, actorType, email, firstName, lastName, null, null, null, roles);
    }

    public IamUserCreated {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(actorType, "actorType");
        roles = roles == null ? List.of() : List.copyOf(roles);
        if (actorType != IamActorType.USER
                && (blank(email) || blank(firstName) || blank(lastName))) {
            throw new IllegalArgumentException("IAM actor event requires email, first_name and last_name");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
