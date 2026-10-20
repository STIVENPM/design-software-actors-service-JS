package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.BrowseInstructorsUseCase;
import co.sena.actors.application.port.in.InstructorPage;
import co.sena.actors.application.port.in.InstructorQuery;
import co.sena.actors.application.port.in.Viewer;
import co.sena.actors.application.port.out.InstructorRepositoryPort;
import co.sena.actors.domain.exception.InstructorNotFoundException;
import co.sena.actors.domain.exception.UnsupportedScopeException;
import co.sena.actors.domain.model.Instructor;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Read side (E2). Owns pagination, filters and visibility so the REST
 * adapter only deserializes HTTP and delegates. TRAINING_CENTER viewers
 * only see instructors whose current contract belongs to their center
 * (HU-ACT-002 join); only the area filter remains unsupported.
 */
public class BrowseInstructorsUseCaseImpl implements BrowseInstructorsUseCase {

    private static final Set<String> SORTABLE = Set.of(
            "createdAt", "updatedAt", "lastName", "firstName", "documentNumber");

    private final InstructorRepositoryPort repository;

    public BrowseInstructorsUseCaseImpl(InstructorRepositoryPort repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public Instructor get(UUID id, Viewer viewer) {
        Access access = resolveAccess(viewer);
        Instructor instructor = repository.findActiveById(id)
                .orElseThrow(() -> new InstructorNotFoundException(id));
        if (access == Access.OWN && !id.equals(viewer.actorId())) {
            throw new InstructorNotFoundException(id);
        }
        if (access == Access.CENTER && !isInCenter(id, centerOf(viewer))) {
            throw new InstructorNotFoundException(id);
        }
        return instructor;
    }

    @Override
    public InstructorPage list(InstructorQuery query, Viewer viewer) {
        Access access = resolveAccess(viewer);
        rejectUnsupportedFilters(query);
        int page = Math.max(query.page(), 1);
        int size = Math.min(Math.max(query.pageSize(), 1), 100);
        String sort = SORTABLE.contains(query.sortProperty()) ? query.sortProperty() : "createdAt";
        if (access == Access.OWN) {
            return ownPage(query, viewer, page, size);
        }
        UUID center = access == Access.CENTER ? centerOf(viewer) : parseCenter(query.trainingCenterId());
        if (access == Access.CENTER && centerMismatch(query.trainingCenterId(), center)) {
            throw new UnsupportedScopeException("ACT_INSTRUCTOR_VIEW", viewer.scopes());
        }
        String documentNumber = normalize(query.documentNumber());
        long total = center == null
                ? repository.countActive(query.active(), documentNumber)
                : repository.countActiveInCenter(query.active(), documentNumber, center);
        List<Instructor> content = total == 0 ? List.of()
                : center == null
                        ? repository.findActiveSlice(
                                query.active(), documentNumber, sort, query.ascending(), (page - 1) * size, size)
                        : repository.findActiveSliceInCenter(
                                query.active(), documentNumber, sort, query.ascending(), (page - 1) * size, size,
                                center);
        return new InstructorPage(content, page, size, total, totalPages(total, size));
    }

    private InstructorPage ownPage(InstructorQuery query, Viewer viewer, int page, int size) {
        if (viewer.actorId() == null || !query.active()) {
            return new InstructorPage(List.of(), page, size, 0, 0);
        }
        Instructor own = repository.findActiveById(viewer.actorId()).orElse(null);
        String documentNumber = normalize(query.documentNumber());
        if (own == null || (documentNumber != null
                && !own.getDocumentNumber().toLowerCase(Locale.ROOT).contains(documentNumber))) {
            return new InstructorPage(List.of(), page, size, 0, 0);
        }
        UUID requested = parseCenter(query.trainingCenterId());
        if (requested != null && !isInCenter(own.getId(), requested)) {
            return new InstructorPage(List.of(), page, size, 0, 0);
        }
        List<Instructor> content = page == 1 ? List.of(own) : List.of();
        return new InstructorPage(content, page, size, 1, 1);
    }

    private boolean isInCenter(UUID instructorId, UUID center) {
        return repository.findCurrentTrainingCenterId(instructorId).map(center::equals).orElse(false);
    }

    private UUID centerOf(Viewer viewer) {
        UUID center = viewer.trainingCenterId();
        if (center == null) {
            throw new UnsupportedScopeException("ACT_INSTRUCTOR_VIEW", viewer.scopes());
        }
        return center;
    }

    private boolean centerMismatch(String requested, UUID center) {
        UUID parsed = parseCenter(requested);
        return parsed != null && !parsed.equals(center);
    }

    private Access resolveAccess(Viewer viewer) {
        Set<String> scopes = viewer.scopes();
        if (scopes.isEmpty() || scopes.contains("GLOBAL")) {
            return Access.FULL;
        }
        if (scopes.equals(Set.of("OWN_PROFILE"))) {
            return Access.OWN;
        }
        if (scopes.equals(Set.of("TRAINING_CENTER"))) {
            return Access.CENTER;
        }
        throw new UnsupportedScopeException("ACT_INSTRUCTOR_VIEW", scopes);
    }

    private void rejectUnsupportedFilters(InstructorQuery query) {
        if (query.area() != null && !query.area().isBlank()) {
            throw new UnsupportedScopeException(
                    "ACT_INSTRUCTOR_VIEW",
                    Set.of("AREA_FILTER_UNSUPPORTED"));
        }
    }

    private UUID parseCenter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return UUID.fromString(raw.trim());
    }

    private int totalPages(long total, int size) {
        if (total == 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / size);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private enum Access {
        FULL, OWN, CENTER
    }
}
