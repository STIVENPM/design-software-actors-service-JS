package co.sena.actors.domain.exception;

import java.util.Set;

/**
 * 403 SCOPE_VIOLATION: the feature is present but no usable scope is.
 * Supported for instructor reads: GLOBAL (or the bare feature),
 * OWN_PROFILE and TRAINING_CENTER (by current contract). Only the
 * area list filter remains unsupported.
 */
public class UnsupportedScopeException extends RuntimeException {

    private final String feature;
    private final Set<String> scopes;

    public UnsupportedScopeException(String feature, Set<String> scopes) {
        super("Scope insuficiente para " + feature + ": " + scopes
                + ". Soportados: GLOBAL, OWN_PROFILE y TRAINING_CENTER (filtro area pendiente)");
        this.feature = feature;
        this.scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
    }

    public String getFeature() {
        return feature;
    }

    public Set<String> getScopes() {
        return scopes;
    }
}
