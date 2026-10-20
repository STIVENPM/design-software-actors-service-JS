package co.sena.actors.adapter.config;

import co.sena.actors.application.port.in.CompanyUseCase;
import co.sena.actors.application.port.out.CompanyRepositoryPort;
import co.sena.actors.application.usecase.CompanyUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** HU-ACT-008 wiring, isolated from previous use-case configuration. */
@Configuration
public class CompanyConfig {
    @Bean
    CompanyUseCase companyUseCase(CompanyRepositoryPort repository) {
        return new CompanyUseCaseImpl(repository);
    }
}
