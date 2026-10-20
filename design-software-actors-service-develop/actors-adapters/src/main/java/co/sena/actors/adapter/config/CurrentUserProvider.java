package co.sena.actors.adapter.config;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Audit UUID (created_by/updated_by/deleted_by) taken from the JWT. */
@Component
public class CurrentUserProvider {

    public static final UUID SYSTEM_ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return SYSTEM_ACTOR_ID;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            UUID fromSub = parseUuid(jwt.getSubject());
            if (fromSub != null) {
                return fromSub;
            }
            UUID fromActor = parseUuid(jwt.getClaimAsString("actor_id"));
            if (fromActor != null) {
                return fromActor;
            }
        }
        return SYSTEM_ACTOR_ID;
    }

    public UUID currentActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return parseUuid(jwt.getClaimAsString("actor_id"));
        }
        return null;
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
