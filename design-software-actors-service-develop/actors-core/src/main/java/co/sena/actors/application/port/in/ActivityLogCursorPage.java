package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.ActivityLogEntry;
import java.util.List;

public record ActivityLogCursorPage(
        List<ActivityLogEntry> content,
        String nextCursor,
        int limit,
        boolean hasNext) {
}
