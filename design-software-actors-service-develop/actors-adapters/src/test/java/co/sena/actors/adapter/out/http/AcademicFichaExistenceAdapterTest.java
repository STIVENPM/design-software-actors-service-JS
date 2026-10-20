package co.sena.actors.adapter.out.http;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import co.sena.actors.application.usecase.FichaLookupUnavailableException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AcademicFichaExistenceAdapterTest {

    private MockRestServiceServer server;
    private AcademicFichaExistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new AcademicFichaExistenceAdapter(builder.baseUrl("http://academic.test").build());
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("sub", "system-admin")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ACADEMIC_FICHA_VIEW:GLOBAL"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void existingFichaUsesInboundJwt() {
        UUID fichaId = UUID.randomUUID();
        server.expect(requestTo("http://academic.test/api/v1/fichas/" + fichaId))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withStatus(HttpStatus.OK));

        assertTrue(adapter.existsById(fichaId));
        server.verify();
    }

    @Test
    void unknownFichaReturnsFalse() {
        UUID fichaId = UUID.randomUUID();
        server.expect(requestTo("http://academic.test/api/v1/fichas/" + fichaId))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertFalse(adapter.existsById(fichaId));
        server.verify();
    }

    @Test
    void forbiddenAcademicLookupIsNotFichaNotFound() {
        UUID fichaId = UUID.randomUUID();
        server.expect(requestTo("http://academic.test/api/v1/fichas/" + fichaId))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(FichaLookupUnavailableException.class, () -> adapter.existsById(fichaId));
        server.verify();
    }

    @Test
    void missingJwtDoesNotCallAcademic() {
        SecurityContextHolder.clearContext();

        assertThrows(FichaLookupUnavailableException.class, () -> adapter.existsById(UUID.randomUUID()));
        server.verify();
    }

    @Test
    void scopedAcademicPermissionCannotProveNonexistence() {
        Jwt jwt = Jwt.withTokenValue("scoped-token")
                .header("alg", "RS256")
                .claim("sub", "coordinator")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ACADEMIC_FICHA_VIEW:TRAINING_CENTER"))));

        assertThrows(FichaLookupUnavailableException.class, () -> adapter.existsById(UUID.randomUUID()));
        server.verify();
    }
}
