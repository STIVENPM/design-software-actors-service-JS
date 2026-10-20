package co.sena.actors.adapter.out.persistence.company;

import co.sena.actors.application.port.in.CompanyUseCase;
import co.sena.actors.application.port.out.CompanyRepositoryPort;
import co.sena.actors.domain.model.Company;
import co.sena.actors.domain.model.CompanyDetails;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class CompanyPersistenceAdapter implements CompanyRepositoryPort {
    private final CompanyJpaRepository repository;

    public CompanyPersistenceAdapter(CompanyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByNit(String nit) { return repository.existsByNit(nit); }

    @Override
    public Optional<Company> findActiveById(UUID id) {
        return repository.findByIdAndActiveTrue(id).map(this::toDomain);
    }

    @Override
    public CompanyUseCase.Page findAll(CompanyUseCase.Query query) {
        Sort sort = Sort.by(query.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC,
                query.sortProperty()).and(Sort.by("id"));
        var page = repository.findAll((root, criteria, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("active"), query.active()));
            if (query.nit() != null) {
                predicates.add(cb.equal(root.get("nit"), query.nit().trim()));
            }
            if (query.cityId() != null) {
                predicates.add(cb.equal(root.get("cityId"), query.cityId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(query.page() - 1, query.pageSize(), sort));
        return new CompanyUseCase.Page(page.getContent().stream().map(this::toDomain).toList(),
                query.page(), query.pageSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional
    public Company save(Company company) {
        var entity = repository.findById(company.id()).orElseGet(CompanyJpaEntity::new);
        if (entity.getId() != null && entity.getRowVersion() != company.rowVersion()) {
            throw new ObjectOptimisticLockingFailureException(CompanyJpaEntity.class, company.id());
        }
        entity.setId(company.id());
        entity.setNit(company.nit());
        var details = company.details();
        entity.setBusinessName(details.businessName());
        entity.setTradeName(details.tradeName());
        entity.setEconomicActivity(details.economicActivity());
        entity.setAddress(details.address());
        entity.setCityId(details.cityId());
        entity.setContactName(details.contactName());
        entity.setContactEmail(details.contactEmail());
        entity.setContactPhone(details.contactPhone());
        entity.setActive(company.active());
        entity.setCreatedAt(company.createdAt());
        entity.setCreatedBy(company.createdBy());
        entity.setUpdatedAt(company.updatedAt());
        entity.setUpdatedBy(company.updatedBy());
        entity.setDeletedAt(company.deletedAt());
        entity.setDeletedBy(company.deletedBy());
        return toDomain(repository.saveAndFlush(entity));
    }

    private Company toDomain(CompanyJpaEntity entity) {
        return new Company(entity.getId(), entity.getNit(),
                new CompanyDetails(entity.getBusinessName(), entity.getTradeName(),
                        entity.getEconomicActivity(), entity.getAddress(), entity.getCityId(),
                        entity.getContactName(), entity.getContactEmail(), entity.getContactPhone()),
                entity.getActive(), entity.getCreatedAt(), entity.getCreatedBy(),
                entity.getUpdatedAt(), entity.getUpdatedBy(), entity.getDeletedAt(),
                entity.getDeletedBy(), entity.getRowVersion());
    }
}
