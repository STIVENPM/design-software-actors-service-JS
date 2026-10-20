package co.sena.actors.adapter.config;

import co.sena.actors.application.port.out.CatalogOutputPort;
import co.sena.actors.domain.instructor.BrowseCatalogDetailsInteractor;
import co.sena.actors.domain.instructor.BrowseCatalogsInteractor;
import co.sena.actors.domain.instructor.GetActiveCatalogReportInteractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public BrowseCatalogsInteractor browseCatalogsInteractor(CatalogOutputPort catalogOutputPort) {
        return new BrowseCatalogsInteractor(catalogOutputPort);
    }

    @Bean
    public BrowseCatalogDetailsInteractor browseCatalogDetailsInteractor(CatalogOutputPort catalogOutputPort) {
        return new BrowseCatalogDetailsInteractor(catalogOutputPort);
    }

    @Bean
    public GetActiveCatalogReportInteractor getActiveCatalogReportInteractor(CatalogOutputPort catalogOutputPort) {
        return new GetActiveCatalogReportInteractor(catalogOutputPort);
    }
}
