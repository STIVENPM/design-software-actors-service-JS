package co.sena.actors.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Company(
        UUID id, String nit, CompanyDetails details, boolean active,
        OffsetDateTime createdAt, UUID createdBy, OffsetDateTime updatedAt, UUID updatedBy,
        OffsetDateTime deletedAt, UUID deletedBy, int rowVersion) {
}
