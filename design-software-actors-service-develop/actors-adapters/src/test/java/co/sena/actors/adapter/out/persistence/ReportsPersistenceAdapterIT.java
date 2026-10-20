package co.sena.actors.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.sena.actors.application.port.in.InstructorLoadReportQuery;
import co.sena.actors.application.port.in.LearnerTrackingReportEntry;
import co.sena.actors.application.port.in.LearnerTrackingReportQuery;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = ReportsPersistenceAdapterIT.TestApplication.class)
@Testcontainers
class ReportsPersistenceAdapterIT {

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
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "public");
        registry.add("spring.jpa.properties.hibernate.jdbc.time_zone", () -> "UTC");
        registry.add("spring.main.allow-bean-definition-overriding", () -> "true");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
        @Import(ReportsPersistenceAdapter.class)
    static class TestApplication {
    }

    @Autowired
    private ReportsPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws SQLException {
        createSchemas();
        createTables();
        seedStatusCatalog();
        seedData();
    }

    @Test
    void learnerTrackingReadsCurrentEnrollmentAndLatestProductiveStage() {
        var result = adapter.findLearnerTracking(new LearnerTrackingReportQuery(
                1, 20, null, null, "ACTIVE", null, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fullName()).isEqualTo("Ana Torres");
    }

    @Test
    void instructorLoadAggregatesCurrentContractAndCounts() {
        var result = adapter.findInstructorLoad(new InstructorLoadReportQuery(
                1, 20, null, null, null, null, null));

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(entry -> {
            assertThat(entry.instructorId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            assertThat(entry.defaultMaxHoursPerWeek()).isEqualByComparingTo(new BigDecimal("40.0"));
            assertThat(entry.currentContractWeeklyHourLimit()).isEqualByComparingTo(new BigDecimal("30.0"));
            assertThat(entry.activeAreasCount()).isEqualTo(2);
            assertThat(entry.activeCompetenciesCount()).isEqualTo(3);
            assertThat(entry.activeExceptionsCount()).isEqualTo(2);
        });
    }

    private void createSchemas() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS actors_learners;");
            statement.execute("CREATE SCHEMA IF NOT EXISTS actors_instructors;");
            statement.execute("CREATE SCHEMA IF NOT EXISTS actors_parameterization;");
            statement.execute("CREATE SCHEMA IF NOT EXISTS actors_companies;");
        }
    }

    private void createTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_parameterization.status_category (id UUID PRIMARY KEY, code VARCHAR(50) NOT NULL UNIQUE)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_parameterization.status (id UUID PRIMARY KEY, code VARCHAR(50) NOT NULL, status_category_id UUID NOT NULL REFERENCES actors_parameterization.status_category(id), is_terminal BOOLEAN NOT NULL DEFAULT false)");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_learners.learner (id UUID PRIMARY KEY, document_number VARCHAR(20) NOT NULL UNIQUE, first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT true, deleted_at TIMESTAMP NULL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_learners.learner_ficha_enrollment (id UUID PRIMARY KEY, learner_id UUID NOT NULL REFERENCES actors_learners.learner(id), ficha_id UUID NOT NULL, enrollment_status_id UUID NOT NULL REFERENCES actors_parameterization.status(id), stage VARCHAR(20) NOT NULL, enrollment_date DATE NOT NULL, completion_date DATE NULL, is_current BOOLEAN NOT NULL DEFAULT false, is_active BOOLEAN NOT NULL DEFAULT true, deleted_at TIMESTAMP NULL)");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_companies.company (id UUID PRIMARY KEY, is_active BOOLEAN NOT NULL DEFAULT true, deleted_at TIMESTAMP NULL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_companies.productive_stage (id UUID PRIMARY KEY, learner_id UUID NOT NULL REFERENCES actors_learners.learner(id), company_id UUID NOT NULL REFERENCES actors_companies.company(id), status_id UUID NOT NULL REFERENCES actors_parameterization.status(id), start_date DATE NOT NULL, is_active BOOLEAN NOT NULL DEFAULT true, deleted_at TIMESTAMP NULL)");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_instructors.instructor (id UUID PRIMARY KEY, document_number VARCHAR(20) NOT NULL UNIQUE, first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL, default_max_hours_per_week NUMERIC(4,1) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT true, deleted_at TIMESTAMP NULL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_instructors.instructor_contract (id UUID PRIMARY KEY, instructor_id UUID NOT NULL REFERENCES actors_instructors.instructor(id), weekly_hour_limit NUMERIC(4,1), training_center_id UUID, is_current BOOLEAN NOT NULL DEFAULT false, is_active BOOLEAN NOT NULL DEFAULT true, deleted_at TIMESTAMP NULL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_instructors.instructor_area (id UUID PRIMARY KEY, instructor_id UUID NOT NULL REFERENCES actors_instructors.instructor(id), area_name VARCHAR(100) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT true, deleted_at TIMESTAMP NULL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_instructors.competency_assignment (id UUID PRIMARY KEY, instructor_id UUID NOT NULL REFERENCES actors_instructors.instructor(id), is_active BOOLEAN NOT NULL DEFAULT true, deleted_at TIMESTAMP NULL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS actors_instructors.instructor_availability_exception (id UUID PRIMARY KEY, instructor_id UUID NOT NULL REFERENCES actors_instructors.instructor(id), start_datetime TIMESTAMP WITH TIME ZONE NOT NULL, end_datetime TIMESTAMP WITH TIME ZONE NOT NULL, is_active BOOLEAN NOT NULL DEFAULT true, deleted_at TIMESTAMP NULL)");
    }

    private void seedStatusCatalog() {
        UUID categoryId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        jdbcTemplate.update("INSERT INTO actors_parameterization.status_category (id, code) VALUES (?, 'LEARNER_ENROLLMENT_STATUS') ON CONFLICT (id) DO NOTHING", categoryId);
        insertStatus(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "ACTIVE", categoryId);
        insertStatus(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), "DROPOUT", categoryId);
        insertStatus(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"), "TRANSFERRED", categoryId);
        insertStatus(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"), "GRADUATED", categoryId);
        insertStatus(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), "CANCELLED", categoryId);
    }

    private void insertStatus(UUID id, String code, UUID categoryId) {
        jdbcTemplate.update(
                "INSERT INTO actors_parameterization.status (id, code, status_category_id, is_terminal) VALUES (?, ?, ?, false) ON CONFLICT (id) DO NOTHING",
                id, code, categoryId);
    }

    private void seedData() {
        UUID learner1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID learner2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID learner3 = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID instructor1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID instructor2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID company1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID stageStatusId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        jdbcTemplate.update("INSERT INTO actors_learners.learner (id, document_number, first_name, last_name, is_active, deleted_at) VALUES (?, '1001', 'Ana', 'Torres', true, null) ON CONFLICT (id) DO NOTHING", learner1);
        jdbcTemplate.update("INSERT INTO actors_learners.learner (id, document_number, first_name, last_name, is_active, deleted_at) VALUES (?, '1002', 'Luis', 'Perez', true, null) ON CONFLICT (id) DO NOTHING", learner2);
        jdbcTemplate.update("INSERT INTO actors_learners.learner (id, document_number, first_name, last_name, is_active, deleted_at) VALUES (?, '1003', 'Inactivo', 'Learner', false, null) ON CONFLICT (id) DO NOTHING", learner3);

        UUID enrollmentStatus1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID enrollmentStatus2 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID enrollmentStatus3 = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

        jdbcTemplate.update("INSERT INTO actors_learners.learner_ficha_enrollment (id, learner_id, ficha_id, enrollment_status_id, stage, enrollment_date, completion_date, is_current, is_active, deleted_at) VALUES (?, ?, ?, ?, 'LECTURE', ?, NULL, true, true, NULL) ON CONFLICT (id) DO NOTHING",
                UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"), learner1, UUID.fromString("aaaaaaaa-0000-0000-0000-000000000101"), enrollmentStatus1, LocalDate.of(2026, 1, 10));
        jdbcTemplate.update("INSERT INTO actors_learners.learner_ficha_enrollment (id, learner_id, ficha_id, enrollment_status_id, stage, enrollment_date, completion_date, is_current, is_active, deleted_at) VALUES (?, ?, ?, ?, 'LECTURE', ?, NULL, true, true, NULL) ON CONFLICT (id) DO NOTHING",
                UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002"), learner2, UUID.fromString("aaaaaaaa-0000-0000-0000-000000000102"), enrollmentStatus2, LocalDate.of(2026, 2, 5));
        jdbcTemplate.update("INSERT INTO actors_learners.learner_ficha_enrollment (id, learner_id, ficha_id, enrollment_status_id, stage, enrollment_date, completion_date, is_current, is_active, deleted_at) VALUES (?, ?, ?, ?, 'LECTURE', ?, NULL, false, true, NULL) ON CONFLICT (id) DO NOTHING",
                UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003"), learner3, UUID.fromString("aaaaaaaa-0000-0000-0000-000000000103"), enrollmentStatus3, LocalDate.of(2026, 3, 10));

        jdbcTemplate.update("INSERT INTO actors_companies.company (id, is_active, deleted_at) VALUES (?, true, NULL) ON CONFLICT (id) DO NOTHING", company1);
        jdbcTemplate.update("INSERT INTO actors_companies.productive_stage (id, learner_id, company_id, status_id, start_date, is_active, deleted_at) VALUES (?, ?, ?, ?, ?, true, NULL) ON CONFLICT (id) DO NOTHING",
                UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001"), learner1, company1, stageStatusId, LocalDate.of(2026, 4, 1));
        jdbcTemplate.update("INSERT INTO actors_companies.productive_stage (id, learner_id, company_id, status_id, start_date, is_active, deleted_at) VALUES (?, ?, ?, ?, ?, true, NULL) ON CONFLICT (id) DO NOTHING",
                UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002"), learner2, company1, stageStatusId, LocalDate.of(2026, 4, 3));

        jdbcTemplate.update("INSERT INTO actors_instructors.instructor (id, document_number, first_name, last_name, default_max_hours_per_week, is_active, deleted_at) VALUES (?, '2001', 'Miguel', 'Rojas', 40.0, true, NULL) ON CONFLICT (id) DO NOTHING", instructor1);
        jdbcTemplate.update("INSERT INTO actors_instructors.instructor (id, document_number, first_name, last_name, default_max_hours_per_week, is_active, deleted_at) VALUES (?, '2002', 'Sara', 'Lopez', 35.0, true, NULL) ON CONFLICT (id) DO NOTHING", instructor2);

        jdbcTemplate.update("INSERT INTO actors_instructors.instructor_contract (id, instructor_id, weekly_hour_limit, training_center_id, is_current, is_active, deleted_at) VALUES (?, ?, 30.0, ?, true, true, NULL) ON CONFLICT (id) DO NOTHING",
                UUID.fromString("cccccccc-0000-0000-0000-000000000001"), instructor1, UUID.fromString("dddddddd-0000-0000-0000-000000000001"));
        jdbcTemplate.update("INSERT INTO actors_instructors.instructor_contract (id, instructor_id, weekly_hour_limit, training_center_id, is_current, is_active, deleted_at) VALUES (?, ?, 25.0, ?, true, true, NULL) ON CONFLICT (id) DO NOTHING",
                UUID.fromString("cccccccc-0000-0000-0000-000000000002"), instructor2, UUID.fromString("dddddddd-0000-0000-0000-000000000002"));

        jdbcTemplate.update("INSERT INTO actors_instructors.instructor_area (id, instructor_id, area_name, is_active, deleted_at) VALUES (?, ?, 'Backend', true, NULL) ON CONFLICT (id) DO NOTHING", UUID.fromString("eeeeeeee-0000-0000-0000-000000000001"), instructor1);
        jdbcTemplate.update("INSERT INTO actors_instructors.instructor_area (id, instructor_id, area_name, is_active, deleted_at) VALUES (?, ?, 'Frontend', true, NULL) ON CONFLICT (id) DO NOTHING", UUID.fromString("eeeeeeee-0000-0000-0000-000000000002"), instructor1);
        jdbcTemplate.update("INSERT INTO actors_instructors.instructor_area (id, instructor_id, area_name, is_active, deleted_at) VALUES (?, ?, 'AI', true, NULL) ON CONFLICT (id) DO NOTHING", UUID.fromString("eeeeeeee-0000-0000-0000-000000000003"), instructor2);

        jdbcTemplate.update("INSERT INTO actors_instructors.competency_assignment (id, instructor_id, is_active, deleted_at) VALUES (?, ?, true, NULL) ON CONFLICT (id) DO NOTHING", UUID.fromString("ffffffff-0000-0000-0000-000000000001"), instructor1);
        jdbcTemplate.update("INSERT INTO actors_instructors.competency_assignment (id, instructor_id, is_active, deleted_at) VALUES (?, ?, true, NULL) ON CONFLICT (id) DO NOTHING", UUID.fromString("ffffffff-0000-0000-0000-000000000002"), instructor1);
        jdbcTemplate.update("INSERT INTO actors_instructors.competency_assignment (id, instructor_id, is_active, deleted_at) VALUES (?, ?, true, NULL) ON CONFLICT (id) DO NOTHING", UUID.fromString("ffffffff-0000-0000-0000-000000000003"), instructor1);
        jdbcTemplate.update("INSERT INTO actors_instructors.competency_assignment (id, instructor_id, is_active, deleted_at) VALUES (?, ?, true, NULL) ON CONFLICT (id) DO NOTHING", UUID.fromString("ffffffff-0000-0000-0000-000000000004"), instructor2);

        jdbcTemplate.update("INSERT INTO actors_instructors.instructor_availability_exception (id, instructor_id, start_datetime, end_datetime, is_active, deleted_at) VALUES (?, ?, ?, ?, true, NULL) ON CONFLICT (id) DO NOTHING",
                UUID.fromString("11111111-0000-0000-0000-000000000001"), instructor1,
                OffsetDateTime.parse("2026-01-10T08:00:00Z"), OffsetDateTime.parse("2026-01-15T17:00:00Z"));
        jdbcTemplate.update("INSERT INTO actors_instructors.instructor_availability_exception (id, instructor_id, start_datetime, end_datetime, is_active, deleted_at) VALUES (?, ?, ?, ?, true, NULL) ON CONFLICT (id) DO NOTHING",
                UUID.fromString("11111111-0000-0000-0000-000000000002"), instructor1,
                OffsetDateTime.parse("2026-02-10T08:00:00Z"), OffsetDateTime.parse("2026-02-15T17:00:00Z"));
        jdbcTemplate.update("INSERT INTO actors_instructors.instructor_availability_exception (id, instructor_id, start_datetime, end_datetime, is_active, deleted_at) VALUES (?, ?, ?, ?, false, NULL) ON CONFLICT (id) DO NOTHING",
                UUID.fromString("11111111-0000-0000-0000-000000000003"), instructor1,
                OffsetDateTime.parse("2026-03-10T08:00:00Z"), OffsetDateTime.parse("2026-03-15T17:00:00Z"));
    }
}
