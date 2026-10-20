package co.sena.actors.adapter.in.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.actors.adapter.config.SecurityConfig;
import co.sena.actors.adapter.out.persistence.LearnerFichaEnrollmentJpaRepository;
import co.sena.actors.adapter.out.persistence.LearnerFichaEnrollmentPersistenceAdapter;
import co.sena.actors.adapter.out.persistence.LearnerJpaRepository;
import co.sena.actors.application.port.in.CountActiveLearnersByFichaUseCase;
import co.sena.actors.application.port.out.FichaExistencePort;
import co.sena.actors.application.port.out.LearnerFichaEnrollmentRepositoryPort;
import co.sena.actors.application.usecase.CountActiveLearnersByFichaUseCaseImpl;
import co.sena.actors.application.usecase.FichaLookupUnavailableException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = InternalFichaLearnerControllerIT.TestApplication.class,
        properties = "actors.security.dev-jwt-decoder=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class InternalFichaLearnerControllerIT {

    private static final UUID ACTIVE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID DROPOUT = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
    private static final UUID TRANSFERRED = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");
    private static final UUID GRADUATED = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000004");
    private static final UUID CANCELLED = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000005");

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("actors_test")
            .withUsername("actors")
            .withPassword("actors");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
            org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
    })
    @Import({InternalFichaLearnerController.class, SecurityConfig.class,
            GlobalExceptionHandler.class, LearnerFichaEnrollmentPersistenceAdapter.class})
    static class TestApplication {
        @MockBean
        LearnerFichaEnrollmentJpaRepository enrollmentRepository;

        @MockBean
        LearnerJpaRepository learnerRepository;

        @Bean
        CountActiveLearnersByFichaUseCase countActiveLearnersByFichaUseCase(
                LearnerFichaEnrollmentRepositoryPort repository, FichaExistencePort fichas) {
            return new CountActiveLearnersByFichaUseCaseImpl(repository, fichas);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private FichaExistencePort fichas;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS actors_learners");
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS actors_parameterization");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_parameterization.status_category (id UUID PRIMARY KEY, code VARCHAR(80) NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_parameterization.status (id UUID PRIMARY KEY, code VARCHAR(80) NOT NULL, status_category_id UUID NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_learners.learner (id UUID PRIMARY KEY, is_active BOOLEAN NOT NULL, deleted_at TIMESTAMPTZ)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_learners.learner_ficha_enrollment (id UUID PRIMARY KEY, learner_id UUID NOT NULL, ficha_id UUID NOT NULL, enrollment_status_id UUID NOT NULL, is_current BOOLEAN NOT NULL, is_active BOOLEAN NOT NULL, deleted_at TIMESTAMPTZ)");
        jdbcTemplate.execute("DELETE FROM actors_learners.learner_ficha_enrollment");
        jdbcTemplate.execute("DELETE FROM actors_learners.learner");
        jdbcTemplate.execute("DELETE FROM actors_parameterization.status");
        jdbcTemplate.execute("DELETE FROM actors_parameterization.status_category");

        UUID category = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO actors_parameterization.status_category (id, code) VALUES (?, ?)",
                category, "LEARNER_ENROLLMENT_STATUS");
        insertStatus(ACTIVE, "ACTIVE", category);
        insertStatus(DROPOUT, "DROPOUT", category);
        insertStatus(TRANSFERRED, "TRANSFERRED", category);
        insertStatus(GRADUATED, "GRADUATED", category);
        insertStatus(CANCELLED, "CANCELLED", category);
    }

    @Test
    @WithMockUser(authorities = "ACT_INTERNAL_FICHA_LEARNER_COUNT")
    void returnsZeroAndSnakeCaseForFichaWithoutEnrollments() throws Exception {
        UUID fichaId = UUID.randomUUID();
        when(fichas.existsById(fichaId)).thenReturn(true);
        expectCount(fichaId, 0);
    }

    @Test
    @WithMockUser(authorities = "ACT_INTERNAL_FICHA_LEARNER_COUNT")
    void returnsNotFoundForUnknownFicha() throws Exception {
        mockMvc.perform(get("/internal/fichas/{fichaId}/active-learners/count", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Ficha not found"))
                .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }

    @Test
    @WithMockUser(authorities = "ACT_INTERNAL_FICHA_LEARNER_COUNT")
    void doesNotTreatAcademicFailureAsUnknownFicha() throws Exception {
        UUID fichaId = UUID.randomUUID();
        when(fichas.existsById(fichaId)).thenThrow(new FichaLookupUnavailableException());
        mockMvc.perform(get("/internal/fichas/{fichaId}/active-learners/count", fichaId))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("DEPENDENCY_UNAVAILABLE"));
    }

    @Test
    @WithMockUser(authorities = "ACT_INTERNAL_FICHA_LEARNER_COUNT")
    void countsOneActiveLearner() throws Exception {
        UUID fichaId = UUID.randomUUID();
        when(fichas.existsById(fichaId)).thenReturn(true);
        insertEnrollment(fichaId, ACTIVE, true, true, false, true, false);
        expectCount(fichaId, 1);
    }

    @Test
    @WithMockUser(authorities = "ACT_INTERNAL_FICHA_LEARNER_COUNT")
    void countsSeveralActiveLearnersOnlyForRequestedFicha() throws Exception {
        UUID fichaId = UUID.randomUUID();
        when(fichas.existsById(fichaId)).thenReturn(true);
        insertEnrollment(fichaId, ACTIVE, true, true, false, true, false);
        insertEnrollment(fichaId, ACTIVE, true, true, false, true, false);
        insertEnrollment(fichaId, ACTIVE, true, true, false, true, false);
        insertEnrollment(UUID.randomUUID(), ACTIVE, true, true, false, true, false);
        expectCount(fichaId, 3);
    }

    @Test
    @WithMockUser(authorities = "ACT_INTERNAL_FICHA_LEARNER_COUNT")
    void excludesHistoricalNonActiveAndDeletedRecords() throws Exception {
        UUID fichaId = UUID.randomUUID();
        when(fichas.existsById(fichaId)).thenReturn(true);
        insertEnrollment(fichaId, ACTIVE, true, true, false, true, false);
        insertEnrollment(fichaId, ACTIVE, false, true, false, true, false);
        insertEnrollment(fichaId, DROPOUT, true, true, false, true, false);
        insertEnrollment(fichaId, TRANSFERRED, true, true, false, true, false);
        insertEnrollment(fichaId, GRADUATED, true, true, false, true, false);
        insertEnrollment(fichaId, CANCELLED, true, true, false, true, false);
        insertEnrollment(fichaId, ACTIVE, true, false, false, true, false);
        insertEnrollment(fichaId, ACTIVE, true, true, true, true, false);
        insertEnrollment(fichaId, ACTIVE, true, true, false, false, false);
        insertEnrollment(fichaId, ACTIVE, true, true, false, true, true);
        expectCount(fichaId, 1);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/internal/fichas/{fichaId}/active-learners/count", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("MISSING_TOKEN"));
    }

    @Test
    @WithMockUser(authorities = "ACT_LEARNER_VIEW")
    void rejectsPublicLearnerFeature() throws Exception {
        mockMvc.perform(get("/internal/fichas/{fichaId}/active-learners/count", UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_PERMISSIONS"));
    }

    private void expectCount(UUID fichaId, int count) throws Exception {
        mockMvc.perform(get("/internal/fichas/{fichaId}/active-learners/count", fichaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ficha_id").value(fichaId.toString()))
                .andExpect(jsonPath("$.active_learners").value(count));
    }

    private void insertStatus(UUID id, String code, UUID category) {
        jdbcTemplate.update("INSERT INTO actors_parameterization.status (id, code, status_category_id) VALUES (?, ?, ?)",
                id, code, category);
    }

    private void insertEnrollment(UUID fichaId, UUID statusId, boolean current, boolean enrollmentActive,
            boolean enrollmentDeleted, boolean learnerActive, boolean learnerDeleted) {
        UUID learnerId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO actors_learners.learner (id, is_active, deleted_at) VALUES (?, ?, ?)",
                learnerId, learnerActive, learnerDeleted ? java.time.OffsetDateTime.now() : null);
        jdbcTemplate.update("""
                INSERT INTO actors_learners.learner_ficha_enrollment
                  (id, learner_id, ficha_id, enrollment_status_id, is_current, is_active, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), learnerId, fichaId, statusId, current, enrollmentActive,
                enrollmentDeleted ? java.time.OffsetDateTime.now() : null);
    }
}
