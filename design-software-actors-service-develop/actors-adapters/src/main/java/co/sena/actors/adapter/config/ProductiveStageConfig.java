package co.sena.actors.adapter.config;

import co.sena.actors.application.port.in.ProductiveStageUseCase;
import co.sena.actors.application.port.out.ProductiveStageRepositoryPort;
import co.sena.actors.application.usecase.ProductiveStageUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductiveStageConfig {
    @Bean
    ProductiveStageUseCase productiveStageUseCase(ProductiveStageRepositoryPort repository) {
        return new ProductiveStageUseCaseImpl(repository);
    }
}
