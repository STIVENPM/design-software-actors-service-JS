package co.sena.actors.application.port.in;

import java.util.Set;
import java.util.UUID;

/**
 * Who is reading. Scopes are the raw JWT scope suffixes
 * (GLOBAL, TRAINING_CENTER, OWN_PROFILE). An empty set means the bare
 * feature without scope, treated as GLOBAL. trainingCenterId carries the
 * JWT training_center_id claim and is only used for TRAINING_CENTER scope
 * (current contract join, HU-ACT-002).
 */
public record Viewer(UUID actorId, Set<String> scopes, UUID trainingCenterId) {

    public Viewer {
        scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
    }

    public Viewer(UUID actorId, Set<String> scopes) {
        this(actorId, scopes, null);
    }
}
