package co.sena.actors.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.sena.actors.adapter.config.CompanyConfig;
import co.sena.actors.adapter.config.CurrentUserProvider;
import co.sena.actors.adapter.config.SecurityConfig;
import co.sena.actors.adapter.out.persistence.company.CompanyJpaEntity;
import co.sena.actors.adapter.out.persistence.company.CompanyJpaRepository;
import co.sena.actors.adapter.out.persistence.company.CompanyPersistenceAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = CompanyControllerIT.TestApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true"
})
@AutoConfigureMockMvc
@Testcontainers
@WithMockUser(authorities = {"ACT_COMPANY_VIEW", "ACT_COMPANY_MANAGE"})
class CompanyControllerIT {
    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = CompanyJpaEntity.class)
    @EnableJpaRepositories(basePackageClasses = CompanyJpaRepository.class)
    @Import({CompanyController.class, CompanyConfig.class, CompanyPersistenceAdapter.class,
            CurrentUserProvider.class, SecurityConfig.class, GlobalExceptionHandler.class})
    static class TestApplication {}

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @MockBean JwtDecoder decoder;

    @BeforeEach
    void cleanCompaniesOnly() {
        jdbc.update("DELETE FROM actors_companies.company");
    }

    private UUID createCompany(String nit) throws Exception {
        var result = mvc.perform(post("/api/v1/companies").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("nit", nit, "business_name", "Empresa " + nit))))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void createsCompanyWithContractFieldsAndAudit() throws Exception {
        UUID actor = UUID.randomUUID();
        UUID city = UUID.randomUUID(); // No physical FK or reference-data fixture.
        var result = mvc.perform(post("/api/v1/companies")
                .with(jwt().jwt(token -> token.subject(actor.toString()))
                        .authorities(new SimpleGrantedAuthority("ACT_COMPANY_MANAGE")))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                        "nit", "900123", "business_name", "Empresa SAS", "trade_name", "Comercial",
                        "economic_activity", "6201", "address", "Calle 1", "city_id", city.toString(),
                        "contact_name", "Ana", "contact_email", "ana@example.com", "contact_phone", "123"))))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nit").value("900123"))
                .andExpect(jsonPath("$.business_name").value("Empresa SAS"))
                .andExpect(jsonPath("$.city_id").value(city.toString()))
                .andExpect(jsonPath("$.contact_email").value("ana@example.com"))
                .andExpect(jsonPath("$.is_active").value(true))
                .andExpect(jsonPath("$.created_at").exists()).andReturn();
        UUID id = UUID.fromString(mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
        assertEquals(actor, jdbc.queryForObject("SELECT created_by FROM actors_companies.company WHERE id=?", UUID.class, id));
        assertEquals(0, jdbc.queryForObject("SELECT row_version FROM actors_companies.company WHERE id=?", Integer.class, id));
    }

    @Test
    void duplicateNitReturns409EvenAfterSoftDelete() throws Exception {
        UUID id = createCompany("DUP");
        mvc.perform(delete("/api/v1/companies/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/companies").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nit\":\" DUP \",\"business_name\":\"Otra\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void listsActiveCompaniesWithPaginationAndInactiveFilter() throws Exception {
        UUID id = createCompany("INACTIVE");
        createCompany("ACTIVE");
        mvc.perform(delete("/api/v1/companies/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/companies")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nit").value("ACTIVE"))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.total_items").value(1))
                .andExpect(jsonPath("$.pagination.page_size").value(20));
        mvc.perform(get("/api/v1/companies").param("is_active", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].nit").value("INACTIVE"));
    }

    @Test
    void filtersByNitAndCityAndSortsPages() throws Exception {
        UUID city = UUID.randomUUID();
        UUID id = createCompany("A");
        createCompany("B");
        mvc.perform(patch("/api/v1/companies/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"city_id\":\"" + city + "\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/companies").param("nit", "A").param("city_id", city.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pagination.total_items").value(1));
        mvc.perform(get("/api/v1/companies").param("sort", "nit:asc").param("page_size", "1").param("page", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].nit").value("B"))
                .andExpect(jsonPath("$.pagination.total_pages").value(2));
    }

    @Test
    void getsCompanyById() throws Exception {
        UUID id = createCompany("GET");
        mvc.perform(get("/api/v1/companies/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void missingCompanyReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(get("/api/v1/companies/{id}", id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mvc.perform(put("/api/v1/companies/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"business_name\":\"Missing\"}")).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/companies/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/companies/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void putReplacesOptionalFieldsAndPreservesNit() throws Exception {
        UUID id = createCompany("PUT");
        mvc.perform(patch("/api/v1/companies/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"trade_name\":\"Anterior\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/companies/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"business_name\":\"Nueva\",\"contact_phone\":\"555\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.business_name").value("Nueva"))
                .andExpect(jsonPath("$.nit").value("PUT")).andExpect(jsonPath("$.contact_phone").value("555"));
        assertNull(jdbc.queryForObject("SELECT trade_name FROM actors_companies.company WHERE id=?", String.class, id));
        assertEquals(2, jdbc.queryForObject("SELECT row_version FROM actors_companies.company WHERE id=?", Integer.class, id));
    }

    @Test
    void patchChangesOnlySuppliedFieldsAndAuditsActor() throws Exception {
        UUID id = createCompany("PATCH");
        UUID actor = UUID.randomUUID();
        mvc.perform(patch("/api/v1/companies/{id}", id)
                .with(jwt().jwt(token -> token.subject(actor.toString()))
                        .authorities(new SimpleGrantedAuthority("ACT_COMPANY_MANAGE")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"contact_name\":\"Ana\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.business_name").value("Empresa PATCH"))
                .andExpect(jsonPath("$.contact_name").value("Ana"));
        assertEquals(actor, jdbc.queryForObject("SELECT updated_by FROM actors_companies.company WHERE id=?", UUID.class, id));
    }

    @Test
    void deletePreservesRowAndStoresAudit() throws Exception {
        UUID id = createCompany("DELETE");
        UUID actor = UUID.randomUUID();
        mvc.perform(delete("/api/v1/companies/{id}", id)
                .with(jwt().jwt(token -> token.subject(actor.toString()))
                        .authorities(new SimpleGrantedAuthority("ACT_COMPANY_MANAGE"))))
                .andExpect(status().isNoContent());
        var row = jdbc.queryForMap("SELECT * FROM actors_companies.company WHERE id=?", id);
        assertEquals(false, row.get("is_active"));
        assertNotNull(row.get("deleted_at"));
        assertEquals(actor, row.get("deleted_by"));
        mvc.perform(get("/api/v1/companies/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void patchCanDeactivateAccordingToContract() throws Exception {
        UUID id = createCompany("DISABLE");
        mvc.perform(patch("/api/v1/companies/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"is_active\":false}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.is_active").value(false));
    }

    static Stream<String> invalidPayloads() {
        return Stream.of("{}", "{\"nit\":\"\",\"business_name\":\"Empresa\"}",
                "{\"nit\":\"" + "1".repeat(21) + "\",\"business_name\":\"Empresa\"}",
                "{\"nit\":\"X\",\"business_name\":\"" + "a".repeat(201) + "\"}",
                "{\"nit\":\"X\",\"business_name\":\"  \"}",
                "{\"nit\":\"X\",\"business_name\":\"Empresa\",\"contact_email\":\"invalid\"}",
                "{\"nit\":\"X\",\"business_name\":\"Empresa\",\"city_id\":\"invalid\"}");
    }

    @ParameterizedTest
    @MethodSource("invalidPayloads")
    void invalidCreateReturns400(String body) throws Exception {
        mvc.perform(post("/api/v1/companies").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsBlankNameOnUpdatesAndInvalidPaging() throws Exception {
        UUID id = createCompany("VALIDATION");
        mvc.perform(put("/api/v1/companies/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/companies/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"business_name\":\" \"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/companies").param("page", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/companies").param("page_size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/companies").param("sort", "unknown:asc")).andExpect(status().isBadRequest());
    }

    static Stream<Arguments> deniedEndpoints() {
        String detail = "/api/v1/companies/" + UUID.randomUUID();
        return Stream.of(
                Arguments.of("GET", "/api/v1/companies", "ACT_COMPANY_MANAGE"),
                Arguments.of("GET", detail, "ACT_COMPANY_MANAGE"),
                Arguments.of("POST", "/api/v1/companies", "ACT_COMPANY_VIEW"),
                Arguments.of("PUT", detail, "ACT_COMPANY_VIEW"),
                Arguments.of("PATCH", detail, "ACT_COMPANY_VIEW"),
                Arguments.of("DELETE", detail, "ACT_COMPANY_VIEW"),
                Arguments.of("GET", "/api/v1/companies", "ACT_LEARNER_VIEW"),
                Arguments.of("POST", "/api/v1/companies", "ACT_LEARNER_MANAGE"));
    }

    @ParameterizedTest
    @MethodSource("deniedEndpoints")
    void enforcesCompanyPermissions(String method, String path, String authority) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), path)
                .with(user("limited").authorities(new SimpleGrantedAuthority(authority)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nit\":\"RBAC\",\"business_name\":\"Empresa\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void unauthenticatedReturns401() throws Exception {
        mvc.perform(get("/api/v1/companies")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/companies").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
