package co.sena.actors.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.sena.actors.adapter.config.CurrentUserProvider;
import co.sena.actors.adapter.config.ProductiveStageConfig;
import co.sena.actors.adapter.config.SecurityConfig;
import co.sena.actors.adapter.out.persistence.productive.CompanyVisitJpaEntity;
import co.sena.actors.adapter.out.persistence.productive.CompanyVisitJpaRepository;
import co.sena.actors.adapter.out.persistence.productive.ProductiveStageJpaEntity;
import co.sena.actors.adapter.out.persistence.productive.ProductiveStageJpaRepository;
import co.sena.actors.adapter.out.persistence.productive.ProductiveStagePersistenceAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = ProductiveStageControllerIT.TestApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@Testcontainers
class ProductiveStageControllerIT {
    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {ProductiveStageJpaEntity.class, CompanyVisitJpaEntity.class})
    @EnableJpaRepositories(basePackageClasses = {ProductiveStageJpaRepository.class, CompanyVisitJpaRepository.class})
    @Import({ProductiveStageController.class, ProductiveStageConfig.class, ProductiveStagePersistenceAdapter.class,
            CurrentUserProvider.class, SecurityConfig.class, GlobalExceptionHandler.class})
    static class TestApplication {}

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withInitScript("productive-stage-schema.sql");

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

    UUID learnerId;
    UUID companyId;
    UUID instructorId;
    UUID activeStatusId;
    UUID terminalStatusId;
    UUID interruptedStatusId;
    UUID otherCategoryStatusId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM actors_companies.company_visit");
        jdbc.update("DELETE FROM actors_companies.productive_stage");
        jdbc.update("DELETE FROM actors_companies.company");
        jdbc.update("DELETE FROM actors_learners.learner");
        jdbc.update("DELETE FROM actors_instructors.instructor");
        jdbc.update("DELETE FROM actors_parameterization.status");
        jdbc.update("DELETE FROM actors_parameterization.status_category");

        learnerId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        instructorId = UUID.randomUUID();
        activeStatusId = UUID.randomUUID();
        terminalStatusId = UUID.randomUUID();
        interruptedStatusId = UUID.randomUUID();
        otherCategoryStatusId = UUID.randomUUID();
        UUID productiveStageCategoryId = UUID.randomUUID();
        UUID otherCategoryId = UUID.randomUUID();
        jdbc.update("INSERT INTO actors_learners.learner (id) VALUES (?)", learnerId);
        jdbc.update("INSERT INTO actors_instructors.instructor (id, is_active) VALUES (?, true)", instructorId);
        jdbc.update("INSERT INTO actors_companies.company (id, is_active) VALUES (?, true)", companyId);
        jdbc.update("INSERT INTO actors_parameterization.status_category (id, code) VALUES (?, 'PRODUCTIVE_STAGE')",
                productiveStageCategoryId);
        jdbc.update("INSERT INTO actors_parameterization.status_category (id, code) VALUES (?, 'LEARNER_ENROLLMENT_STATUS')",
                otherCategoryId);
        jdbc.update("INSERT INTO actors_parameterization.status (id, code, status_category_id, is_terminal) VALUES (?, 'ACTIVE', ?, false)",
                activeStatusId, productiveStageCategoryId);
        jdbc.update("INSERT INTO actors_parameterization.status (id, code, status_category_id, is_terminal) VALUES (?, 'COMPLETED', ?, true)",
                terminalStatusId, productiveStageCategoryId);
        jdbc.update("INSERT INTO actors_parameterization.status (id, code, status_category_id, is_terminal) VALUES (?, 'INTERRUPTED', ?, true)",
                interruptedStatusId, productiveStageCategoryId);
        jdbc.update("INSERT INTO actors_parameterization.status (id, code, status_category_id, is_terminal) VALUES (?, 'ACTIVE', ?, false)",
                otherCategoryStatusId, otherCategoryId);
    }

    @Test
    void createsProductiveStage() throws Exception {
        var id = createStage();
        mvc.perform(get("/api/v1/productive-stages/{id}", id).with(viewUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learner_id").value(learnerId.toString()))
                .andExpect(jsonPath("$.company_id").value(companyId.toString()))
                .andExpect(jsonPath("$.total_hours_completed").value(0));
    }

    @Test
    void rejectsPlannedEndBeforeStart() throws Exception {
        mvc.perform(post("/api/v1/productive-stages").with(manageUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(stageJson(LocalDate.now(), LocalDate.now().minusDays(1))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejectsCreateWithStatusFromAnotherCategory() throws Exception {
        mvc.perform(post("/api/v1/productive-stages").with(manageUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(stageJson(otherCategoryStatusId, LocalDate.now(), LocalDate.now().plusDays(90))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updatesHoursAndFilters() throws Exception {
        var id = createStage();
        mvc.perform(put("/api/v1/productive-stages/{id}", id).with(manageUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"total_hours_completed\":120}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_hours_completed").value(120));
        mvc.perform(get("/api/v1/productive-stages").with(viewUser())
                .param("learner_id", learnerId.toString())
                .param("company_id", companyId.toString())
                .param("status_id", activeStatusId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(1));
    }

    @Test
    void rejectsNegativeHours() throws Exception {
        var id = createStage();
        mvc.perform(put("/api/v1/productive-stages/{id}", id).with(manageUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"total_hours_completed\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUpdateWithStatusFromAnotherCategory() throws Exception {
        var id = createStage();
        mvc.perform(put("/api/v1/productive-stages/{id}", id).with(manageUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status_id\":\"" + otherCategoryStatusId + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createsVisitAndMarksLateFirstVisit() throws Exception {
        var id = createStage();
        mvc.perform(post("/api/v1/productive-stages/{id}/visits", id).with(manageUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(visitJson(1, "IN_PERSON", "GOOD", LocalDate.now().plusDays(40))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.first_visit_late").value(true))
                .andExpect(jsonPath("$.has_improvement_plan").value(false));
    }

    @ParameterizedTest
    @MethodSource("invalidVisitPayloads")
    void rejectsInvalidVisitPayloads(String body) throws Exception {
        var id = createStage();
        mvc.perform(post("/api/v1/productive-stages/{id}/visits", id).with(manageUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejectsInvalidVisitNumberWithBeanValidation() throws Exception {
        var id = createStage();
        mvc.perform(post("/api/v1/productive-stages/{id}/visits", id).with(manageUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"visit_number\":0,\"visit_date\":\"" + LocalDate.now()
                        + "\",\"visit_type\":\"IN_PERSON\",\"learner_performance\":\"GOOD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blocksTerminalStatusWithLessThanTwoVisits() throws Exception {
        var id = createStage();
        mvc.perform(put("/api/v1/productive-stages/{id}", id).with(manageUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status_id\":\"" + terminalStatusId + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void allowsTerminalStatusAfterTwoVisits() throws Exception {
        var id = createStage();
        addVisit(id, 1);
        addVisit(id, 2);
        mvc.perform(put("/api/v1/productive-stages/{id}", id).with(manageUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status_id\":\"" + terminalStatusId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_id").value(terminalStatusId.toString()));
    }

    @Test
    void interruptedRequiresReason() throws Exception {
        var id = createStage();
        addVisit(id, 1);
        addVisit(id, 2);
        mvc.perform(put("/api/v1/productive-stages/{id}", id).with(manageUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status_id\":\"" + interruptedStatusId + "\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/v1/productive-stages/{id}", id).with(manageUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status_id\":\"" + interruptedStatusId + "\",\"interruption_reason\":\"Cambio de contrato\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interruption_reason").value("Cambio de contrato"));
    }

    @Test
    void enforcesPermissions() throws Exception {
        mvc.perform(get("/api/v1/productive-stages").with(manageUser()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/productive-stages").with(viewUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(stageJson(LocalDate.now(), LocalDate.now().plusDays(90))))
                .andExpect(status().isForbidden());
    }

    private UUID createStage() throws Exception {
        var result = mvc.perform(post("/api/v1/productive-stages").with(manageUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(stageJson(LocalDate.now(), LocalDate.now().plusDays(90))))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private void addVisit(UUID id, int number) throws Exception {
        mvc.perform(post("/api/v1/productive-stages/{id}/visits", id).with(manageUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(visitJson(number, "VIRTUAL", "GOOD", LocalDate.now().plusDays(number))))
                .andExpect(status().isCreated());
    }

    private String stageJson(LocalDate start, LocalDate end) throws Exception {
        return stageJson(activeStatusId, start, end);
    }

    private String stageJson(UUID statusId, LocalDate start, LocalDate end) throws Exception {
        return mapper.writeValueAsString(Map.of(
                "learner_id", learnerId.toString(),
                "company_id", companyId.toString(),
                "supervisor_instructor_id", instructorId.toString(),
                "status_id", statusId.toString(),
                "start_date", start.toString(),
                "planned_end_date", end.toString(),
                "total_hours_required", 864));
    }

    private String visitJson(int number, String type, String performance, LocalDate date) {
        return "{\"visit_number\":" + number + ",\"visit_date\":\"" + date
                + "\",\"visit_type\":\"" + type + "\",\"learner_performance\":\""
                + performance + "\",\"company_satisfaction\":\"HIGH\"}";
    }

    static Stream<String> invalidVisitPayloads() {
        return Stream.of(
                "{\"visit_number\":1,\"visit_date\":\"" + LocalDate.now() + "\",\"visit_type\":\"CHAT\",\"learner_performance\":\"GOOD\"}",
                "{\"visit_number\":1,\"visit_date\":\"" + LocalDate.now() + "\",\"visit_type\":\"PHONE\",\"learner_performance\":\"UNKNOWN\"}",
                "{\"visit_number\":1,\"visit_date\":\"" + LocalDate.now() + "\",\"visit_type\":\"PHONE\",\"learner_performance\":\"GOOD\",\"company_satisfaction\":\"BAD\"}");
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor manageUser() {
        return user("manager").authorities(new SimpleGrantedAuthority("ACT_PRODUCTIVE_STAGE_MANAGE"));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor viewUser() {
        return user("viewer").authorities(new SimpleGrantedAuthority("ACT_ACTIVITY_LOG_VIEW"));
    }
}
