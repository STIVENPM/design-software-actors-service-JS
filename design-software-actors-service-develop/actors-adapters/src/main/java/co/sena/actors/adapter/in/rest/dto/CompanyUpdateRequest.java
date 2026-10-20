package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.domain.model.CompanyDetails;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.*;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
/** PUT replaces optional fields; PATCH ignores null fields, following existing CRUD. */
public record CompanyUpdateRequest(
        @Size(max = 200) String businessName,
        @Size(max = 200) String tradeName,
        @Size(max = 100) String economicActivity,
        String address,
        UUID cityId,
        @Size(max = 200) String contactName,
        @Email String contactEmail,
        @Size(max = 20) String contactPhone,
        Boolean isActive) {
    public CompanyDetails toDetails() {
        return new CompanyDetails(businessName, tradeName, economicActivity, address,
                cityId, contactName, contactEmail, contactPhone);
    }
}
