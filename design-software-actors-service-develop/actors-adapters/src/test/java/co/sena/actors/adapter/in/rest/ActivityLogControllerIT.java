package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.out.persistence.ActivityLogJpaEntity;
import co.sena.actors.adapter.out.persistence.ActivityLogJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    classes = ActivityLogControllerIT.TestApplication.class,
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "actors.security.dev-jwt-decoder=true"
    }
)
@AutoConfigureMockMvc
@Testcontainers
@org.springframework.test.context.ActiveProfiles("test")
class ActivityLogControllerIT {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "co.sena.actors")
    @EnableJpaRepositories(basePackages = "co.sena.actors.adapter.out.persistence")
    @org.springframework.context.annotation.Import({
        co.sena.actors.adapter.in.rest.InstructorController.class,
        co.sena.actors.adapter.in.rest.LearnerController.class,
        co.sena.actors.adapter.in.rest.InstructorAuth.class,
        co.sena.actors.adapter.in.rest.GlobalExceptionHandler.class,
        co.sena.actors.adapter.config.CurrentUserProvider.class,
        co.sena.actors.adapter.config.SecurityConfig.class,
        co.sena.actors.adapter.config.UseCaseConfig.class,
        co.sena.actors.adapter.out.persistence.ActivityLogPersistenceAdapter.class
    })
    static class TestApplication {
        // Mock any external dependencies that other UseCases might need if UseCaseConfig loads them all
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.InstructorRepositoryPort instructorRepositoryPort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.InstructorAreaRepositoryPort instructorAreaRepositoryPort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.InstructorContractRepositoryPort instructorContractRepositoryPort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.CompetencyAssignmentRepositoryPort competencyAssignmentRepositoryPort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.LearnerRepositoryPort learnerRepositoryPort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.LearnerEnrollmentPort learnerEnrollmentPort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.LearnerFichaEnrollmentRepositoryPort learnerFichaEnrollmentRepositoryPort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.FichaExistencePort fichaExistencePort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.ImprovementPlanRepositoryPort improvementPlanRepositoryPort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.LearnerEnrollmentEventPublisherPort learnerEnrollmentEventPublisherPort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.core.port.out.InstructorAvailabilityExceptionRepositoryPort instructorAvailabilityExceptionRepositoryPort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.core.port.out.AvailabilityEventPublisherPort availabilityEventPublisherPort;
    }

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("actors_test")
            .withUsername("actors")
            .withPassword("actors");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.create_namespaces", () -> "true");

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_tracking;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_learners;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_instructors;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_parameterization;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS parameterization;");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create schema", e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActivityLogJpaRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        repository.deleteAll();
        try {
            jdbcTemplate.execute("ALTER TABLE actors_tracking.activity_log DROP CONSTRAINT IF EXISTS ck_activity_log_actor");
            jdbcTemplate.execute("ALTER TABLE actors_tracking.activity_log ADD CONSTRAINT ck_activity_log_actor CHECK ((instructor_id IS NOT NULL) <> (learner_id IS NOT NULL))");
        } catch (Exception ignored) {
        }
    }

    @Test
    void shouldEnforceDbConstraint() {
        ActivityLogJpaEntity entity = new ActivityLogJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setInstructorId(UUID.randomUUID());
        entity.setLearnerId(UUID.randomUUID()); // Both not null triggers check
        entity.setEventType("TEST_EVENT");
        entity.setDescription("Test constraint");
        entity.setRecordedAt(OffsetDateTime.now());

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            repository.saveAndFlush(entity);
        });
    }

    @Test
    @WithMockUser(authorities = "ACT_ACTIVITY_LOG_VIEW")
    void shouldFindInstructorActivityLogViaHttp() throws Exception {
        UUID instructorId = UUID.randomUUID();
        
        OffsetDateTime now = OffsetDateTime.now();
        for (int i = 0; i < 5; i++) {
            ActivityLogJpaEntity entity = new ActivityLogJpaEntity();
            entity.setId(UUID.randomUUID());
            entity.setInstructorId(instructorId);
            entity.setEventType("TEST_EVENT");
            entity.setDescription("Test Event " + i);
            entity.setRecordedAt(now.minusDays(i));
            repository.saveAndFlush(entity);
        }

        String response = mockMvc.perform(get("/api/v1/instructors/{id}/activity-log", instructorId)
                .param("limit", "3")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.pagination.has_more").value(true))
                .andExpect(jsonPath("$.pagination.next_cursor").isNotEmpty())
                .andExpect(jsonPath("$.pagination.limit").value(3))
                .andReturn().getResponse().getContentAsString();

        String nextCursor = response.split("\"next_cursor\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/v1/instructors/{id}/activity-log", instructorId)
                .param("limit", "3")
                .param("cursor", nextCursor)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.pagination.has_more").value(false));
    }

    @Test
    @WithMockUser(authorities = "ACT_ACTIVITY_LOG_VIEW")
    void shouldFindLearnerActivityLogViaHttp() throws Exception {
        UUID learnerId = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();
        for (int i = 0; i < 5; i++) {
            ActivityLogJpaEntity entity = new ActivityLogJpaEntity();
            entity.setId(UUID.randomUUID());
            entity.setLearnerId(learnerId);
            entity.setEventType("TEST_EVENT");
            entity.setDescription("Learner Event " + i);
            entity.setRecordedAt(now.minusDays(i));
            repository.saveAndFlush(entity);
        }

        String response = mockMvc.perform(get("/api/v1/learners/{id}/activity-log", learnerId)
                .param("limit", "3")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.pagination.has_more").value(true))
                .andExpect(jsonPath("$.pagination.next_cursor").isNotEmpty())
                .andExpect(jsonPath("$.pagination.limit").value(3))
                .andReturn().getResponse().getContentAsString();

        String nextCursor = response.split("\"next_cursor\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/v1/learners/{id}/activity-log", learnerId)
                .param("limit", "3")
                .param("cursor", nextCursor)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.pagination.has_more").value(false));
    }

    @Test
    @WithMockUser(authorities = "ACT_ACTIVITY_LOG_VIEW")
    void shouldReturn400ForInvalidCursor() throws Exception {
        UUID instructorId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/instructors/{id}/activity-log", instructorId)
                .param("cursor", "invalid-date-format")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/instructors/{id}/activity-log", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "OTHER_ROLE")
    void shouldReturn403WithoutCorrectRole() throws Exception {
        mockMvc.perform(get("/api/v1/instructors/{id}/activity-log", UUID.randomUUID()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/learners/{id}/activity-log", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
