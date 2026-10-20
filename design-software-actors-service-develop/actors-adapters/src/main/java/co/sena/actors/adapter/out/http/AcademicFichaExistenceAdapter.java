package co.sena.actors.adapter.out.http;

import co.sena.actors.application.port.out.FichaExistencePort;
import co.sena.actors.application.usecase.FichaLookupUnavailableException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AcademicFichaExistenceAdapter implements FichaExistencePort {

    private final RestClient client;

    public AcademicFichaExistenceAdapter(@Qualifier("academicFichaRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    public boolean existsById(UUID fichaId) {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt)) {
            throw new FichaLookupUnavailableException();
        }
        boolean canReadAllFichas = jwt.getAuthorities().stream()
                .anyMatch(authority -> "ACADEMIC_FICHA_VIEW:GLOBAL".equals(authority.getAuthority()));
        if (!canReadAllFichas) {
            throw new FichaLookupUnavailableException();
        }

        try {
            var response = client.get()
                    .uri("/api/v1/fichas/{id}", fichaId)
                    .headers(headers -> headers.setBearerAuth(jwt.getToken().getTokenValue()))
                    .retrieve()
                    .toBodilessEntity();
            if (response.getStatusCode().value() != HttpStatus.OK.value()) {
                throw new FichaLookupUnavailableException();
            }
            return true;
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        } catch (RestClientException ex) {
            throw new FichaLookupUnavailableException(ex);
        }
    }
}
