package co.sena.actors.application.port.in;

/**
 * List query. sortProperty uses domain names
 * (createdAt, updatedAt, lastName, firstName, documentNumber);
 * the REST adapter translates the API names (created_at, ...).
 * trainingCenterId mirrors the actors.yaml list filter and is enforced
 * against the current contract (HU-ACT-002 join). area is still
 * unsupported: the read use case rejects it explicitly instead of
 * silently ignoring it.
 */
public record InstructorQuery(
        int page,
        int pageSize,
        String sortProperty,
        boolean ascending,
        String documentNumber,
        boolean active,
        String area,
        String trainingCenterId) {
}
