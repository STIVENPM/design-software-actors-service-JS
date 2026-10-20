package co.sena.actors.application.port.out;

import co.sena.actors.application.port.in.CompanyUseCase;
import co.sena.actors.domain.model.Company;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepositoryPort {
    boolean existsByNit(String nit);
    Optional<Company> findActiveById(UUID id);
    Company save(Company company);
    CompanyUseCase.Page findAll(CompanyUseCase.Query query);
}
