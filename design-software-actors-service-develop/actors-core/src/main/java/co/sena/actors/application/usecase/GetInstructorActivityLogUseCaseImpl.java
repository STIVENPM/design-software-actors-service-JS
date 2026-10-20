package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.ActivityLogCursorPage;
import co.sena.actors.application.port.in.GetInstructorActivityLogUseCase;
import co.sena.actors.application.port.in.Viewer;
import co.sena.actors.application.port.out.ActivityLogRepositoryPort;
import co.sena.actors.domain.model.ActivityLogEntry;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class GetInstructorActivityLogUseCaseImpl implements GetInstructorActivityLogUseCase {

    private final ActivityLogRepositoryPort repository;

    public GetInstructorActivityLogUseCaseImpl(ActivityLogRepositoryPort repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public ActivityLogCursorPage getInstructorActivityLog(UUID instructorId, String cursor, int limit, Viewer viewer) {
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);
        // We fetch limit + 1 to know if there's a next page
        List<ActivityLogEntry> entries = repository.findByInstructorIdWithCursor(instructorId, cursor, effectiveLimit + 1);

        boolean hasNext = entries.size() > effectiveLimit;
        if (hasNext) {
            entries = entries.subList(0, effectiveLimit);
        }

        String nextCursor = null;
        if (!entries.isEmpty() && hasNext) {
            nextCursor = entries.get(entries.size() - 1).getRecordedAt().toString();
        }

        return new ActivityLogCursorPage(entries, nextCursor, effectiveLimit, hasNext);
    }
}
