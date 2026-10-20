package co.sena.actors.adapter.in.rest;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Feature-based RBAC (JWT claim features[] shaped as FEATURE:SCOPE).
 * VIEW: SYSTEM_ADMIN/CENTER_DIRECTOR/COORDINATOR (GLOBAL or TRAINING_CENTER)
 * or INSTRUCTOR (OWN_PROFILE only → WHERE id = actor_id).
 * MANAGE: SYSTEM_ADMIN/CENTER_DIRECTOR.
 */
@Component("instructorAuth")
@SuppressWarnings("null") // GrantedAuthority lambdas carry no null contract; editor-only hints, javac is clean
public class InstructorAuth {

    public static final String VIEW = "ACT_INSTRUCTOR_VIEW";
    public static final String MANAGE = "ACT_INSTRUCTOR_MANAGE";

    public boolean canView() {
        return hasFeature(VIEW);
    }

    public boolean canManage() {
        return hasFeature(MANAGE);
    }

    /**
     * Builds the application-layer viewer: actor id, raw JWT scopes and the
     * training_center_id claim. TRAINING_CENTER viewers are filtered by the
     * read use case against the current contract (HU-ACT-002 join).
     */
    public co.sena.actors.application.port.in.Viewer viewer() {
        Set<String> scopes = scopesOf(VIEW);
        UUID actorId = null;
        UUID trainingCenterId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            actorId = claimAsUuid(jwt, "actor_id");
            trainingCenterId = claimAsUuid(jwt, "training_center_id");
        }
        return new co.sena.actors.application.port.in.Viewer(actorId, scopes, trainingCenterId);
    }

    private UUID claimAsUuid(org.springframework.security.oauth2.jwt.Jwt jwt, String claim) {
        try {
            String raw = jwt.getClaimAsString(claim);
            return raw == null || raw.isBlank() ? null : UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean hasFeature(String feature) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String granted = authority.getAuthority();
            if (granted.equals(feature) || granted.startsWith(feature + ":")) {
                return true;
            }
        }
        return false;
    }

    private Set<String> scopesOf(String feature) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(granted -> granted.startsWith(feature + ":"))
                .map(granted -> granted.substring(feature.length() + 1))
                .collect(Collectors.toSet());
    }
}
