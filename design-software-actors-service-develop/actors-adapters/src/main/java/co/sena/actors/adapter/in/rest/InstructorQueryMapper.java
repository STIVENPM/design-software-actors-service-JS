package co.sena.actors.adapter.in.rest;

import co.sena.actors.application.port.in.InstructorQuery;
import java.util.Map;

/**
 * Translates HTTP list parameters (actors.yaml snake_case names)
 * into the application-layer query. Lives in the adapter so the
 * controller only deserializes and delegates, and the use case
 * only sees domain sort names.
 */
public final class InstructorQueryMapper {

    /** API sort names (actors.yaml) to domain sort properties. */
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "created_at", "createdAt",
            "updated_at", "updatedAt",
            "last_name", "lastName",
            "first_name", "firstName",
            "document_number", "documentNumber");

    private InstructorQueryMapper() {
    }

    public static InstructorQuery toQuery(
            int page,
            int pageSize,
            String sort,
            String documentNumber,
            Boolean active,
            String area,
            String trainingCenterId) {
        return new InstructorQuery(
                page,
                pageSize,
                toSortProperty(sort),
                isAscending(sort),
                documentNumber,
                active == null || active,
                area,
                trainingCenterId);
    }

    private static String toSortProperty(String sort) {
        if (sort == null || sort.isBlank()) {
            return "createdAt";
        }
        return SORT_PROPERTIES.getOrDefault(sort.trim().split(":")[0].trim(), "createdAt");
    }

    private static boolean isAscending(String sort) {
        if (sort == null || sort.isBlank() || !sort.contains(":")) {
            return false;
        }
        return sort.trim().split(":")[1].trim().equalsIgnoreCase("asc");
    }
}
