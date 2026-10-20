package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.Instructor;
import java.util.List;

/** Paginated read result (shape mirrors actors.yaml list responses). */
public record InstructorPage(
        List<Instructor> content,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}
