package co.sena.actors.application.port.in;

import java.util.List;

public record ReportPage<T>(
        List<T> data,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}