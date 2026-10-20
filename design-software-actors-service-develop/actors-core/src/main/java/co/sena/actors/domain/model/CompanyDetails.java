package co.sena.actors.domain.model;

import java.util.UUID;

/** Editable company fields from CompanyUpdate; NIT is immutable. */
public record CompanyDetails(
        String businessName, String tradeName, String economicActivity, String address,
        UUID cityId, String contactName, String contactEmail, String contactPhone) {
}
