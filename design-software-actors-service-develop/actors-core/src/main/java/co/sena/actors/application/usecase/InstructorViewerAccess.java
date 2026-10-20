package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.Viewer;
import co.sena.actors.application.port.out.InstructorRepositoryPort;
import co.sena.actors.domain.exception.InstructorNotFoundException;
import co.sena.actors.domain.exception.UnsupportedScopeException;
import co.sena.actors.domain.model.Instructor;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Shared visibility rule for instructor resources (base, areas, contracts).
 * Same semantics as the base reads: unknown/inactive/out-of-scope parent
 * answers 404 (no existence leak); malformed scope answers 403.
 */
public class InstructorViewerAccess {

    private final InstructorRepositoryPort instructors;

    public InstructorViewerAccess(InstructorRepositoryPort instructors) {
        this.instructors = Objects.requireNonNull(instructors, "instructors");
    }

    public Instructor requireVisible(Viewer viewer, UUID instructorId) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(instructorId, "instructorId");
        Instructor instructor = instructors.findActiveById(instructorId)
                .orElseThrow(() -> new InstructorNotFoundException(instructorId));
        Set<String> scopes = viewer.scopes();
        if (scopes.isEmpty() || scopes.contains("GLOBAL")) {
            return instructor;
        }
        if (scopes.equals(Set.of("OWN_PROFILE"))) {
            if (instructorId.equals(viewer.actorId())) {
                return instructor;
            }
            throw new InstructorNotFoundException(instructorId);
        }
        if (scopes.equals(Set.of("TRAINING_CENTER"))) {
            UUID center = viewer.trainingCenterId();
            if (center == null) {
                throw new UnsupportedScopeException("ACT_INSTRUCTOR_VIEW", scopes);
            }
            boolean visible = instructors.findCurrentTrainingCenterId(instructorId)
                    .map(center::equals).orElse(false);
            if (visible) {
                return instructor;
            }
            throw new InstructorNotFoundException(instructorId);
        }
        throw new UnsupportedScopeException("ACT_INSTRUCTOR_VIEW", scopes);
    }
}
