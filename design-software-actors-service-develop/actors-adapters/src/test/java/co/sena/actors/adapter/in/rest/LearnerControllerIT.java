package co.sena.actors.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.actors.adapter.in.rest.dto.LearnerFichaEnrollmentCreateRequest;
import co.sena.actors.adapter.in.rest.dto.LearnerCreateRequest;
import co.sena.actors.adapter.out.persistence.LearnerJpaRepository;
import co.sena.actors.domain.model.DocumentType;
import co.sena.actors.domain.model.LearnerEnrollmentStage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = LearnerControllerIT.TestApplication.class,
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "actors.security.dev-jwt-decoder=true"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class LearnerControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "co.sena.actors")
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "co.sena.actors.adapter.out.persistence")
    @org.springframework.context.annotation.Import({
        co.sena.actors.adapter.in.rest.LearnerController.class,
        co.sena.actors.adapter.in.rest.InstructorAuth.class,
        co.sena.actors.adapter.in.rest.GlobalExceptionHandler.class,
        co.sena.actors.adapter.config.CurrentUserProvider.class,
        co.sena.actors.adapter.config.SecurityConfig.class,
        co.sena.actors.adapter.config.UseCaseConfig.class,
        co.sena.actors.adapter.out.persistence.LearnerPersistenceAdapter.class,
        co.sena.actors.adapter.out.persistence.LearnerFichaEnrollmentPersistenceAdapter.class,
        co.sena.actors.adapter.out.persistence.LearnerEnrollmentPersistenceAdapter.class
    })
    static class TestApplication {
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.InstructorRepositoryPort instructorRepositoryPort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.InstructorAreaRepositoryPort instructorAreaRepositoryPort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.InstructorContractRepositoryPort instructorContractRepositoryPort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.CompetencyAssignmentRepositoryPort competencyAssignmentRepositoryPort;
        @org.springframework.boot.test.mock.mockito.MockBean
        co.sena.actors.application.port.out.ActivityLogRepositoryPort activityLogRepositoryPort;
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

    @org.testcontainers.junit.jupiter.Container
    @SuppressWarnings("resource")
    static org.testcontainers.containers.PostgreSQLContainer<?> postgres = new org.testcontainers.containers.PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("actors_test")
            .withUsername("actors")
            .withPassword("actors");

    static {
        postgres.start();
    }

    @org.springframework.test.context.DynamicPropertySource
    static void configureProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.create_namespaces", () -> "true");

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_learners;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_parameterization;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS parameterization;");
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private LearnerJpaRepository repository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private co.sena.actors.application.port.out.LearnerEnrollmentEventPublisherPort learnerEnrollmentEventPublisherPort;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_parameterization.status_category(id UUID PRIMARY KEY, code VARCHAR(50))");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS actors_parameterization.status(
                    id UUID PRIMARY KEY,
                    code VARCHAR(50),
                    status_category_id UUID,
                    is_initial BOOLEAN,
                    is_terminal BOOLEAN,
                    display_order INTEGER,
                    color_hex VARCHAR(20)
                )
                """);
        jdbcTemplate.execute("ALTER TABLE actors_parameterization.status ADD COLUMN IF NOT EXISTS status_category_id UUID");
        jdbcTemplate.execute("ALTER TABLE actors_parameterization.status ADD COLUMN IF NOT EXISTS is_initial BOOLEAN");
        jdbcTemplate.execute("ALTER TABLE actors_parameterization.status ADD COLUMN IF NOT EXISTS is_terminal BOOLEAN");
        jdbcTemplate.execute("ALTER TABLE actors_parameterization.status ADD COLUMN IF NOT EXISTS display_order INTEGER");
        jdbcTemplate.execute("ALTER TABLE actors_parameterization.status ADD COLUMN IF NOT EXISTS color_hex VARCHAR(20)");
        org.mockito.Mockito.reset(learnerEnrollmentEventPublisherPort);
        
        // Clean up learner test data to avoid FK conflicts between test runs
        jdbcTemplate.execute("DELETE FROM actors_learners.learner_ficha_enrollment");
        repository.deleteAll();
        
        UUID categoryId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID statusId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        
        jdbcTemplate.update("INSERT INTO actors_parameterization.status_category (id, code) VALUES (?, 'LEARNER_ENROLLMENT_STATUS') ON CONFLICT DO NOTHING", categoryId);
        jdbcTemplate.update("INSERT INTO actors_parameterization.status (id, code, status_category_id) VALUES (?, 'CANCELLED', ?) ON CONFLICT DO NOTHING", statusId, categoryId);
        jdbcTemplate.update("INSERT INTO actors_parameterization.status (id, code, status_category_id, is_initial, is_terminal, display_order, color_hex) VALUES (?, 'ACTIVE', ?, true, false, 10, '#16A34A') ON CONFLICT DO NOTHING",
                activeStatusId(), categoryId);
        jdbcTemplate.update("INSERT INTO actors_parameterization.status (id, code, status_category_id, is_initial, is_terminal, display_order, color_hex) VALUES (?, 'TRANSFERRED', ?, false, true, 30, '#2563EB') ON CONFLICT DO NOTHING",
                transferredStatusId(), categoryId);
    }

    @Test
    @WithMockUser(authorities = "ACT_LEARNER_MANAGE")
    void shouldCreateLearner() throws Exception {
        var request = new LearnerCreateRequest(
                UUID.randomUUID(),
                DocumentType.CC,
                "123456789",
                "Juan",
                "Perez",
                "juan@example.com",
                "5551234",
                LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.document_number").value("123456789"));
    }

    @Test
    @WithMockUser(authorities = "ACT_LEARNER_MANAGE")
    void shouldReturn409WhenDocumentNumberExists() throws Exception {
        var request = new LearnerCreateRequest(
                UUID.randomUUID(),
                DocumentType.CC,
                "987654321",
                "Juan",
                "Perez",
                "juan@example.com",
                "5551234",
                LocalDate.of(1990, 1, 1));
        
        mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    @WithMockUser(authorities = "ACT_LEARNER_VIEW")
    void shouldReturn404WhenLearnerNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/learners/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/learners"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ACT_LEARNER_VIEW")
    void shouldBrowseLearnersWithEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/learners?page=1&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.pagination.total_items").exists());
    }

    @Test
    @WithMockUser(authorities = {"ACT_LEARNER_VIEW", "ACT_LEARNER_MANAGE"})
    void shouldBrowseLearnersWithFichaIdAndEnrollmentStatusId() throws Exception {
        // Create 2 learners
        var request1 = new LearnerCreateRequest(UUID.randomUUID(), DocumentType.CC, "F1-1", "A", "B", "a@b.com", "1", LocalDate.now());
        var request2 = new LearnerCreateRequest(UUID.randomUUID(), DocumentType.CC, "F2-1", "C", "D", "c@d.com", "2", LocalDate.now());
        
        String res1 = mockMvc.perform(post("/api/v1/learners").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request1))).andReturn().getResponse().getContentAsString();
        String res2 = mockMvc.perform(post("/api/v1/learners").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request2))).andReturn().getResponse().getContentAsString();
        
        UUID learner1 = UUID.fromString(objectMapper.readTree(res1).get("id").asText());
        UUID learner2 = UUID.fromString(objectMapper.readTree(res2).get("id").asText());

        UUID targetFicha = UUID.randomUUID();
        UUID targetStatus = activeStatusId();

        // learner1 gets targetFicha and targetStatus
        jdbcTemplate.update("INSERT INTO actors_learners.learner_ficha_enrollment (id, learner_id, ficha_id, enrollment_status_id, stage, enrollment_date, is_current) VALUES (?, ?, ?, ?, 'LECTURE', ?, true)", UUID.randomUUID(), learner1, targetFicha, targetStatus, LocalDate.now());
        
        // learner2 gets different ficha
        jdbcTemplate.update("INSERT INTO actors_learners.learner_ficha_enrollment (id, learner_id, ficha_id, enrollment_status_id, stage, enrollment_date, is_current) VALUES (?, ?, ?, ?, 'LECTURE', ?, true)", UUID.randomUUID(), learner2, UUID.randomUUID(), targetStatus, LocalDate.now());

        // test ficha filter
        mockMvc.perform(get("/api/v1/learners?ficha_id=" + targetFicha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(learner1.toString()));

        // test status filter
        mockMvc.perform(get("/api/v1/learners?enrollment_status_id=" + targetStatus))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "ACT_LEARNER_MANAGE")
    void shouldCancelLearnerEnrollment() throws Exception {
        // Create learner first
        var request = new LearnerCreateRequest(
                UUID.randomUUID(), DocumentType.CC, "777888999", "A", "B", "a@b.com", "123", LocalDate.now());
        String response = mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        String idStr = objectMapper.readTree(response).get("id").asText();
        UUID learnerId = UUID.fromString(idStr);

        // Find CANCELLED status ID
        UUID cancelledId = jdbcTemplate.queryForObject(
            "SELECT s.id FROM actors_parameterization.status s JOIN actors_parameterization.status_category c ON s.status_category_id = c.id WHERE s.code = 'CANCELLED' AND c.code = 'LEARNER_ENROLLMENT_STATUS'", UUID.class);

        // Manually insert an enrollment to be able to delete it
        UUID enrollmentId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO actors_learners.learner_ficha_enrollment (id, learner_id, ficha_id, enrollment_status_id, stage, enrollment_date, is_current) VALUES (?, ?, ?, ?, 'LECTURE', ?, true)",
            enrollmentId, learnerId, UUID.randomUUID(), activeStatusId(), LocalDate.now());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/learners/{id}", learnerId))
                .andExpect(status().isNoContent());

        // Assert DB state
        Boolean isCurrent = jdbcTemplate.queryForObject("SELECT is_current FROM actors_learners.learner_ficha_enrollment WHERE id = ?", Boolean.class, enrollmentId);
        UUID currentStatus = jdbcTemplate.queryForObject("SELECT enrollment_status_id FROM actors_learners.learner_ficha_enrollment WHERE id = ?", UUID.class, enrollmentId);
        
        org.junit.jupiter.api.Assertions.assertTrue(isCurrent);
        org.junit.jupiter.api.Assertions.assertEquals(cancelledId, currentStatus);
    }

    @Test
    @WithMockUser(authorities = "ACT_LEARNER_MANAGE")
    void shouldReturnErrorWhenCancelingWithNoActiveEnrollment() throws Exception {
        var request = new LearnerCreateRequest(UUID.randomUUID(), DocumentType.CC, "NO-ENROLL", "A", "B", "a@b.com", "123", LocalDate.now());
        String res = mockMvc.perform(post("/api/v1/learners").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andReturn().getResponse().getContentAsString();
        UUID learnerId = UUID.fromString(objectMapper.readTree(res).get("id").asText());

        // We expect a 400 because of IllegalArgumentException handled by GlobalExceptionHandler
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/learners/{id}", learnerId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ACT_LEARNER_MANAGE")
    void shouldCreateFichaEnrollmentAndPublishEvent() throws Exception {
        UUID learnerId = createLearner("ENR-1");
        UUID fichaId = UUID.randomUUID();

        var request = new LearnerFichaEnrollmentCreateRequest(
                fichaId,
                activeStatusId(),
                LearnerEnrollmentStage.LECTURE,
                LocalDate.of(2026, 9, 17),
                null);

        mockMvc.perform(post("/api/v1/learners/{id}/ficha-enrollments", learnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.learner_id").value(learnerId.toString()))
                .andExpect(jsonPath("$.ficha_id").value(fichaId.toString()))
                .andExpect(jsonPath("$.stage").value("LECTURE"))
                .andExpect(jsonPath("$.is_current").value(true));

        org.mockito.Mockito.verify(learnerEnrollmentEventPublisherPort)
                .publishLearnerEnrolledEvent(org.mockito.ArgumentMatchers.argThat(
                        enrollment -> enrollment.learnerId().equals(learnerId)
                                && enrollment.fichaId().equals(fichaId)));
    }

    @Test
    @WithMockUser(authorities = "ACT_LEARNER_MANAGE")
    void shouldTransferByClosingCurrentAsTransferredAndCreatingActiveEnrollment() throws Exception {
        UUID learnerId = createLearner("ENR-2");
        UUID currentEnrollmentId = UUID.randomUUID();
        UUID destinationFichaId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO actors_learners.learner_ficha_enrollment (id, learner_id, ficha_id, enrollment_status_id, stage, enrollment_date, is_current) VALUES (?, ?, ?, ?, 'INDUCTION', ?, true)",
                currentEnrollmentId, learnerId, UUID.randomUUID(), activeStatusId(), LocalDate.of(2026, 9, 1));

        var request = new LearnerFichaEnrollmentCreateRequest(
                destinationFichaId,
                transferredStatusId(),
                LearnerEnrollmentStage.LECTURE,
                LocalDate.of(2026, 9, 17),
                "Cambio de ficha");

        mockMvc.perform(post("/api/v1/learners/{id}/ficha-enrollments", learnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        Boolean current = jdbcTemplate.queryForObject(
                "SELECT is_current FROM actors_learners.learner_ficha_enrollment WHERE id = ?",
                Boolean.class,
                currentEnrollmentId);
        LocalDate completionDate = jdbcTemplate.queryForObject(
                "SELECT completion_date FROM actors_learners.learner_ficha_enrollment WHERE id = ?",
                LocalDate.class,
                currentEnrollmentId);
        UUID previousStatus = jdbcTemplate.queryForObject(
                "SELECT enrollment_status_id FROM actors_learners.learner_ficha_enrollment WHERE id = ?",
                UUID.class,
                currentEnrollmentId);
        UUID newStatus = jdbcTemplate.queryForObject(
                "SELECT enrollment_status_id FROM actors_learners.learner_ficha_enrollment WHERE learner_id = ? AND ficha_id = ?",
                UUID.class,
                learnerId,
                destinationFichaId);
        Boolean newCurrent = jdbcTemplate.queryForObject(
                "SELECT is_current FROM actors_learners.learner_ficha_enrollment WHERE learner_id = ? AND ficha_id = ?",
                Boolean.class,
                learnerId,
                destinationFichaId);
        String transferReason = jdbcTemplate.queryForObject(
                "SELECT transfer_reason FROM actors_learners.learner_ficha_enrollment WHERE learner_id = ? AND ficha_id = ?",
                String.class,
                learnerId,
                destinationFichaId);
        Integer currentTransferredCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM actors_learners.learner_ficha_enrollment WHERE learner_id = ? AND is_current = true AND enrollment_status_id = ?",
                Integer.class,
                learnerId,
                transferredStatusId());

        org.junit.jupiter.api.Assertions.assertFalse(current);
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2026, 9, 17), completionDate);
        org.junit.jupiter.api.Assertions.assertEquals(transferredStatusId(), previousStatus);
        org.junit.jupiter.api.Assertions.assertTrue(newCurrent);
        org.junit.jupiter.api.Assertions.assertEquals(activeStatusId(), newStatus);
        org.junit.jupiter.api.Assertions.assertEquals("Cambio de ficha", transferReason);
        org.junit.jupiter.api.Assertions.assertEquals(0, currentTransferredCount);
    }

    @Test
    @WithMockUser(authorities = "ACT_LEARNER_MANAGE")
    void shouldRejectDuplicateLearnerFichaEnrollment() throws Exception {
        UUID learnerId = createLearner("ENR-3");
        UUID fichaId = UUID.randomUUID();
        var request = new LearnerFichaEnrollmentCreateRequest(
                fichaId,
                activeStatusId(),
                LearnerEnrollmentStage.LECTURE,
                LocalDate.of(2026, 9, 17),
                null);

        mockMvc.perform(post("/api/v1/learners/{id}/ficha-enrollments", learnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/learners/{id}/ficha-enrollments", learnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    @WithMockUser(authorities = "ACT_LEARNER_MANAGE")
    void shouldReturn422WhenNewEnrollmentWouldCloseCurrentBeforeItsStartDate() throws Exception {
        UUID learnerId = createLearner("ENR-4");
        jdbcTemplate.update(
                "INSERT INTO actors_learners.learner_ficha_enrollment (id, learner_id, ficha_id, enrollment_status_id, stage, enrollment_date, is_current) VALUES (?, ?, ?, ?, 'INDUCTION', ?, true)",
                UUID.randomUUID(), learnerId, UUID.randomUUID(), activeStatusId(), LocalDate.of(2026, 9, 20));

        var request = new LearnerFichaEnrollmentCreateRequest(
                UUID.randomUUID(),
                activeStatusId(),
                LearnerEnrollmentStage.LECTURE,
                LocalDate.of(2026, 9, 17),
                null);

        mockMvc.perform(post("/api/v1/learners/{id}/ficha-enrollments", learnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("UNPROCESSABLE_ENTITY"));
    }

    @Test
    void shouldRequireLearnerViewForFichaEnrollmentHistory() throws Exception {
        mockMvc.perform(get("/api/v1/learners/{id}/ficha-enrollments", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"ACT_LEARNER_VIEW", "ACT_LEARNER_MANAGE"})
    void shouldReturnFichaEnrollmentHistory() throws Exception {
        UUID learnerId = createLearner("ENR-5");
        UUID firstFicha = UUID.randomUUID();
        UUID secondFicha = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO actors_learners.learner_ficha_enrollment (id, learner_id, ficha_id, enrollment_status_id, stage, enrollment_date, completion_date, is_current) VALUES (?, ?, ?, ?, 'INDUCTION', ?, ?, false)",
                UUID.randomUUID(), learnerId, firstFicha, transferredStatusId(), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 17));
        jdbcTemplate.update(
                "INSERT INTO actors_learners.learner_ficha_enrollment (id, learner_id, ficha_id, enrollment_status_id, stage, enrollment_date, is_current) VALUES (?, ?, ?, ?, 'LECTURE', ?, true)",
                UUID.randomUUID(), learnerId, secondFicha, activeStatusId(), LocalDate.of(2026, 9, 17));

        mockMvc.perform(get("/api/v1/learners/{id}/ficha-enrollments", learnerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ficha_id").value(secondFicha.toString()))
                .andExpect(jsonPath("$[1].ficha_id").value(firstFicha.toString()));
    }

    @Test
    @WithMockUser(authorities = {"ACT_LEARNER_VIEW", "ACT_LEARNER_MANAGE"})
    void shouldFilterLearnersByEnrollmentStatusFromFichaEnrollment() throws Exception {
        UUID learnerId = createLearner("ENR-6");
        UUID otherLearnerId = createLearner("ENR-7");
        jdbcTemplate.update(
                "INSERT INTO actors_learners.learner_ficha_enrollment (id, learner_id, ficha_id, enrollment_status_id, stage, enrollment_date, is_current) VALUES (?, ?, ?, ?, 'LECTURE', ?, true)",
                UUID.randomUUID(), learnerId, UUID.randomUUID(), activeStatusId(), LocalDate.now());
        jdbcTemplate.update(
                "INSERT INTO actors_learners.learner_ficha_enrollment (id, learner_id, ficha_id, enrollment_status_id, stage, enrollment_date, is_current) VALUES (?, ?, ?, ?, 'LECTURE', ?, true)",
                UUID.randomUUID(), otherLearnerId, UUID.randomUUID(), transferredStatusId(), LocalDate.now());

        mockMvc.perform(get("/api/v1/learners?enrollment_status_id=" + activeStatusId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(learnerId.toString()));
    }

    private UUID createLearner(String documentNumber) throws Exception {
        var request = new LearnerCreateRequest(
                UUID.randomUUID(),
                DocumentType.CC,
                documentNumber,
                "Ana",
                "Lopez",
                documentNumber.toLowerCase() + "@example.com",
                "555",
                LocalDate.of(2000, 1, 1));

        String response = mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private UUID activeStatusId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000003");
    }

    private UUID transferredStatusId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000004");
    }
}
