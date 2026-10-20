package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.CompanyUseCase;
import co.sena.actors.application.port.out.CompanyRepositoryPort;
import co.sena.actors.domain.exception.CompanyNotFoundException;
import co.sena.actors.domain.exception.DuplicateCompanyException;
import co.sena.actors.domain.model.Company;
import co.sena.actors.domain.model.CompanyDetails;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

public class CompanyUseCaseImpl implements CompanyUseCase {
    private final CompanyRepositoryPort repository;

    public CompanyUseCaseImpl(CompanyRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Company create(String nit, CompanyDetails details, UUID actor) {
        if (nit == null || nit.isBlank() || nit.length() > 20) {
            throw new IllegalArgumentException("nit is required (max 20)");
        }
        validate(details);
        String normalizedNit = nit.trim();
        if (repository.existsByNit(normalizedNit)) {
            throw new DuplicateCompanyException();
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return repository.save(new Company(UUID.randomUUID(), normalizedNit, details, true,
                now, actor, now, actor, null, null, 0));
    }

    @Override
    public Company get(UUID id) {
        return repository.findActiveById(id).orElseThrow(CompanyNotFoundException::new);
    }

    @Override
    public Page list(Query query) {
        if (query.page() < 1 || query.pageSize() < 1 || query.pageSize() > 100) {
            throw new IllegalArgumentException("page must be >= 1 and page_size between 1 and 100");
        }
        if (!Set.of("createdAt", "updatedAt", "nit", "businessName").contains(query.sortProperty())) {
            throw new IllegalArgumentException("Unsupported company sort");
        }
        return repository.findAll(query);
    }

    @Override
    public Company update(UUID id, CompanyDetails incoming, Boolean active, boolean partial, UUID actor) {
        Company current = get(id);
        CompanyDetails previous = current.details();
        // As with existing CRUD PATCH DTOs, null means omitted.
        CompanyDetails details = partial ? new CompanyDetails(
                value(incoming.businessName(), previous.businessName()),
                value(incoming.tradeName(), previous.tradeName()),
                value(incoming.economicActivity(), previous.economicActivity()),
                value(incoming.address(), previous.address()),
                value(incoming.cityId(), previous.cityId()),
                value(incoming.contactName(), previous.contactName()),
                value(incoming.contactEmail(), previous.contactEmail()),
                value(incoming.contactPhone(), previous.contactPhone())) : incoming;
        validate(details);
        boolean enabled = active == null ? current.active() : active;
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return repository.save(new Company(current.id(), current.nit(), details, enabled,
                current.createdAt(), current.createdBy(), now, actor,
                enabled ? null : now, enabled ? null : actor, current.rowVersion()));
    }

    @Override
    public void deactivate(UUID id, UUID actor) {
        update(id, new CompanyDetails(null, null, null, null, null, null, null, null),
                false, true, actor);
    }

    private static <T> T value(T incoming, T previous) {
        return incoming == null ? previous : incoming;
    }

    private static void validate(CompanyDetails details) {
        if (details == null || details.businessName() == null || details.businessName().isBlank()) {
            throw new IllegalArgumentException("business_name is required");
        }
        max(details.businessName(), 200, "business_name");
        max(details.tradeName(), 200, "trade_name");
        max(details.economicActivity(), 100, "economic_activity");
        max(details.contactName(), 200, "contact_name");
        max(details.contactPhone(), 20, "contact_phone");
    }

    private static void max(String value, int length, String field) {
        if (value != null && value.length() > length) {
            throw new IllegalArgumentException(field + " max " + length + " characters");
        }
    }
}
