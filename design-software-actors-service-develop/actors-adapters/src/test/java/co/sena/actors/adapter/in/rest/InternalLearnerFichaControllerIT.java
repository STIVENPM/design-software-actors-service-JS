package co.sena.actors.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.actors.adapter.config.SecurityConfig;
import co.sena.actors.adapter.out.persistence.LearnerFichaEnrollmentJpaRepository;
import co.sena.actors.adapter.out.persistence.LearnerFichaEnrollmentPersistenceAdapter;
import co.sena.actors.adapter.out.persistence.LearnerJpaRepository;
import co.sena.actors.application.port.in.GetActiveFichaByUserUseCase;
import co.sena.actors.application.port.out.LearnerFichaEnrollmentRepositoryPort;
import co.sena.actors.application.port.out.LearnerRepositoryPort;
import co.sena.actors.application.usecase.GetActiveFichaByUserUseCaseImpl;
import co.sena.actors.domain.model.DocumentType;
import co.sena.actors.domain.model.Learner;
import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.security.auth.x500.X500Principal;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = InternalLearnerFichaControllerIT.TestApplication.class,
        properties = "actors.security.dev-jwt-decoder=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class InternalLearnerFichaControllerIT {

    private static final UUID ACTIVE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID DROPOUT = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
    private static final UUID TRANSFERRED = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");
    private static final UUID GRADUATED = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000004");
    private static final UUID CANCELLED = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000005");
    private static final X509Certificate[] IAM_CERTIFICATE = {new TestCertificate("CN=iam-service")};
    private static final X509Certificate[] UNTRUSTED_CERTIFICATE = {new TestCertificate("CN=other-service")};

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
    @Import({InternalLearnerFichaController.class, SecurityConfig.class,
            GlobalExceptionHandler.class, LearnerFichaEnrollmentPersistenceAdapter.class})
    static class TestApplication {
        @MockBean
        LearnerFichaEnrollmentJpaRepository enrollmentRepository;

        @MockBean
        LearnerJpaRepository learnerJpaRepository;

        @Bean
        LearnerRepositoryPort learnerRepositoryPort(JdbcTemplate jdbcTemplate) {
            return new LearnerRepositoryPort() {
                @Override
                public Learner save(Learner learner) {
                    throw new UnsupportedOperationException("Not needed in this IT");
                }

                @Override
                public Optional<Learner> findById(UUID id) {
                    return Optional.empty();
                }

                @Override
                public Optional<Learner> findByUserId(UUID userId) {
                    return findActiveByUserId(userId);
                }

                @Override
                public Optional<Learner> findActiveByUserId(UUID userId) {
                    return jdbcTemplate.query("""
                            SELECT id, user_id, is_active
                            FROM actors_learners.learner
                            WHERE user_id = ?
                              AND is_active = true
                              AND deleted_at IS NULL
                            """, (rs, rowNum) -> new Learner(
                                    (UUID) rs.getObject("id"),
                                    (UUID) rs.getObject("user_id"),
                                    DocumentType.CC,
                                    "DOC-" + rowNum,
                                    "QA",
                                    "Learner",
                                    "qa.learner@example.test",
                                    null,
                                    null,
                                    rs.getBoolean("is_active"),
                                    LocalDateTime.now()), userId)
                            .stream()
                            .findFirst();
                }

                @Override
                public boolean existsByDocumentNumber(String documentNumber) {
                    return false;
                }

                @Override
                public co.sena.actors.application.port.in.QueryLearnerUseCase.PaginatedResult<Learner> findPaginated(
                        co.sena.actors.application.port.in.QueryLearnerUseCase.BrowseQuery query) {
                    throw new UnsupportedOperationException("Not needed in this IT");
                }
            };
        }

        @Bean
        GetActiveFichaByUserUseCase getActiveFichaByUserUseCase(
                LearnerRepositoryPort learnerRepository,
                LearnerFichaEnrollmentRepositoryPort enrollmentRepository) {
            return new GetActiveFichaByUserUseCaseImpl(learnerRepository, enrollmentRepository);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS actors_learners");
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS actors_parameterization");
        jdbcTemplate.execute("DROP TABLE IF EXISTS actors_learners.learner_ficha_enrollment");
        jdbcTemplate.execute("DROP TABLE IF EXISTS actors_learners.learner");
        jdbcTemplate.execute("DROP TABLE IF EXISTS actors_parameterization.status");
        jdbcTemplate.execute("DROP TABLE IF EXISTS actors_parameterization.status_category");
        jdbcTemplate.execute("CREATE TABLE actors_parameterization.status_category (id UUID PRIMARY KEY, code VARCHAR(80) NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE actors_parameterization.status (id UUID PRIMARY KEY, code VARCHAR(80) NOT NULL, status_category_id UUID NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE actors_learners.learner (id UUID PRIMARY KEY, user_id UUID NOT NULL, is_active BOOLEAN NOT NULL, deleted_at TIMESTAMPTZ)");
        jdbcTemplate.execute("CREATE TABLE actors_learners.learner_ficha_enrollment (id UUID PRIMARY KEY, learner_id UUID NOT NULL, ficha_id UUID NOT NULL, enrollment_status_id UUID NOT NULL, is_current BOOLEAN NOT NULL, is_active BOOLEAN NOT NULL, deleted_at TIMESTAMPTZ, enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE)");

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
    void returnsActiveFichaForLearnerUserWithOneActiveEnrollment() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID learnerId = insertLearner(userId, true, false);
        UUID fichaId = UUID.randomUUID();
        insertEnrollment(learnerId, fichaId, ACTIVE, true, true, false);

        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", userId)
                        .requestAttr("jakarta.servlet.request.X509Certificate", IAM_CERTIFICATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learner_id").value(learnerId.toString()))
                .andExpect(jsonPath("$.ficha_id").value(fichaId.toString()));
    }

    @Test
    void returnsNoContentForActiveLearnerWithoutActiveFicha() throws Exception {
        UUID userId = UUID.randomUUID();
        insertLearner(userId, true, false);

        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", userId)
                        .requestAttr("jakarta.servlet.request.X509Certificate", IAM_CERTIFICATE))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsNotFoundWhenUserHasNoLearnerProfile() throws Exception {
        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", UUID.randomUUID())
                        .requestAttr("jakarta.servlet.request.X509Certificate", IAM_CERTIFICATE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void returnsConflictWhenLearnerHasMoreThanOneActiveFicha() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID learnerId = insertLearner(userId, true, false);
        insertEnrollment(learnerId, UUID.randomUUID(), ACTIVE, true, true, false);
        insertEnrollment(learnerId, UUID.randomUUID(), ACTIVE, true, true, false);

        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", userId)
                        .requestAttr("jakarta.servlet.request.X509Certificate", IAM_CERTIFICATE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void excludesInactiveHistoricalDeletedAndNonActiveStatusEnrollments() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID learnerId = insertLearner(userId, true, false);
        UUID activeFichaId = UUID.randomUUID();
        insertEnrollment(learnerId, activeFichaId, ACTIVE, true, true, false);
        insertEnrollment(learnerId, UUID.randomUUID(), ACTIVE, false, true, false);
        insertEnrollment(learnerId, UUID.randomUUID(), ACTIVE, true, false, false);
        insertEnrollment(learnerId, UUID.randomUUID(), ACTIVE, true, true, true);
        insertEnrollment(learnerId, UUID.randomUUID(), DROPOUT, true, true, false);
        insertEnrollment(learnerId, UUID.randomUUID(), TRANSFERRED, true, true, false);
        insertEnrollment(learnerId, UUID.randomUUID(), GRADUATED, true, true, false);
        insertEnrollment(learnerId, UUID.randomUUID(), CANCELLED, true, true, false);

        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", userId)
                        .requestAttr("jakarta.servlet.request.X509Certificate", IAM_CERTIFICATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ficha_id").value(activeFichaId.toString()));
    }

    @Test
    void ignoresInactiveOrDeletedLearnerProfiles() throws Exception {
        UUID inactiveUserId = UUID.randomUUID();
        UUID deletedUserId = UUID.randomUUID();
        UUID inactiveLearnerId = insertLearner(inactiveUserId, false, false);
        UUID deletedLearnerId = insertLearner(deletedUserId, true, true);
        insertEnrollment(inactiveLearnerId, UUID.randomUUID(), ACTIVE, true, true, false);
        insertEnrollment(deletedLearnerId, UUID.randomUUID(), ACTIVE, true, true, false);

        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", inactiveUserId)
                        .requestAttr("jakarta.servlet.request.X509Certificate", IAM_CERTIFICATE))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", deletedUserId)
                        .requestAttr("jakarta.servlet.request.X509Certificate", IAM_CERTIFICATE))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidUuidBeforeLookup() throws Exception {
        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", "not-a-uuid")
                        .requestAttr("jakarta.servlet.request.X509Certificate", IAM_CERTIFICATE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsRequestWithoutTrustedIamCertificateEvenWithBearerToken() throws Exception {
        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", UUID.randomUUID())
                        .header("Authorization", "Bearer fake"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("MTLS_CERTIFICATE_REQUIRED"));
    }

    @Test
    void rejectsUntrustedCertificate() throws Exception {
        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", UUID.randomUUID())
                        .requestAttr("jakarta.servlet.request.X509Certificate", UNTRUSTED_CERTIFICATE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("MTLS_CERTIFICATE_UNTRUSTED"));
    }

    @Test
    void technicalDatabaseErrorIsControlledAndNotReturnedAsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        insertLearner(userId, true, false);
        jdbcTemplate.execute("DROP TABLE actors_learners.learner_ficha_enrollment");

        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", userId)
                        .requestAttr("jakarta.servlet.request.X509Certificate", IAM_CERTIFICATE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
    }

    private void insertStatus(UUID id, String code, UUID category) {
        jdbcTemplate.update("INSERT INTO actors_parameterization.status (id, code, status_category_id) VALUES (?, ?, ?)",
                id, code, category);
    }

    private UUID insertLearner(UUID userId, boolean active, boolean deleted) {
        UUID learnerId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO actors_learners.learner (id, user_id, is_active, deleted_at) VALUES (?, ?, ?, ?)",
                learnerId, userId, active, deleted ? java.time.OffsetDateTime.now() : null);
        return learnerId;
    }

    private void insertEnrollment(UUID learnerId, UUID fichaId, UUID statusId, boolean current,
            boolean enrollmentActive, boolean enrollmentDeleted) {
        jdbcTemplate.update("""
                INSERT INTO actors_learners.learner_ficha_enrollment
                  (id, learner_id, ficha_id, enrollment_status_id, is_current, is_active, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), learnerId, fichaId, statusId, current, enrollmentActive,
                enrollmentDeleted ? java.time.OffsetDateTime.now() : null);
    }

    private static final class TestCertificate extends X509Certificate {
        private final X500Principal principal;

        private TestCertificate(String subject) {
            this.principal = new X500Principal(subject);
        }

        @Override public X500Principal getSubjectX500Principal() { return principal; }
        @Override public X500Principal getIssuerX500Principal() { return principal; }
        @Override public void checkValidity() {}
        @Override public void checkValidity(Date date) {}
        @Override public int getVersion() { return 3; }
        @Override public BigInteger getSerialNumber() { return BigInteger.ONE; }
        @Override public Principal getIssuerDN() { return principal; }
        @Override public Principal getSubjectDN() { return principal; }
        @Override public Date getNotBefore() { return new Date(0); }
        @Override public Date getNotAfter() { return new Date(Long.MAX_VALUE); }
        @Override public byte[] getTBSCertificate() { return new byte[0]; }
        @Override public byte[] getSignature() { return new byte[0]; }
        @Override public String getSigAlgName() { return "NONE"; }
        @Override public String getSigAlgOID() { return "0.0"; }
        @Override public byte[] getSigAlgParams() { return new byte[0]; }
        @Override public boolean[] getIssuerUniqueID() { return null; }
        @Override public boolean[] getSubjectUniqueID() { return null; }
        @Override public boolean[] getKeyUsage() { return null; }
        @Override public int getBasicConstraints() { return -1; }
        @Override public byte[] getEncoded() { return new byte[0]; }
        @Override public void verify(PublicKey key) {}
        @Override public void verify(PublicKey key, String sigProvider) {}
        @Override public String toString() { return principal.getName(); }
        @Override public PublicKey getPublicKey() { return null; }
        @Override public boolean hasUnsupportedCriticalExtension() { return false; }
        @Override public Set<String> getCriticalExtensionOIDs() { return Set.of(); }
        @Override public Set<String> getNonCriticalExtensionOIDs() { return Set.of(); }
        @Override public byte[] getExtensionValue(String oid) { return null; }
    }
}
