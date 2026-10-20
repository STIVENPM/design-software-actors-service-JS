package co.sena.actors.domain.exception;

import java.util.UUID;

public class ImprovementPlanNotFoundException extends RuntimeException {
    public ImprovementPlanNotFoundException(UUID id) {
        super("Improvement plan not found: " + id);
    }
}