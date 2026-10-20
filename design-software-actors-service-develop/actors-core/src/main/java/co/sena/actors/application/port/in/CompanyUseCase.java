package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.Company;
import co.sena.actors.domain.model.CompanyDetails;
import java.util.List;
import java.util.UUID;

public interface CompanyUseCase {
    Company create(String nit, CompanyDetails details, UUID actor);
    Company get(UUID id);
    Page list(Query query);
    Company update(UUID id, CompanyDetails details, Boolean active, boolean partial, UUID actor);
    void deactivate(UUID id, UUID actor);

    record Query(int page, int pageSize, String sortProperty, boolean ascending,
                 String nit, UUID cityId, boolean active) {}
    record Page(List<Company> content, int page, int pageSize, long totalItems, int totalPages) {}
}
