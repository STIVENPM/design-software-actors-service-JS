package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.domain.model.Company;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CompanyResponse(
        UUID id, String nit, String businessName, String tradeName, String economicActivity,
        String address, UUID cityId, String contactName, String contactEmail, String contactPhone,
        boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static CompanyResponse from(Company company) {
        var d = company.details();
        return new CompanyResponse(company.id(), company.nit(), d.businessName(), d.tradeName(),
                d.economicActivity(), d.address(), d.cityId(), d.contactName(), d.contactEmail(),
                d.contactPhone(), company.active(), company.createdAt(), company.updatedAt());
    }
}
